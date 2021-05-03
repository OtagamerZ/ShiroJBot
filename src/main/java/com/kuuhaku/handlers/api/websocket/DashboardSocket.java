/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.handlers.api.websocket;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.api.endpoint.payload.ReadyData;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.common.TempCache;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.BiContract;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.tuple.Pair;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DashboardSocket extends WebSocketServer {
	private final TempCache<String, BiContract<WebSocket, ReadyData>> requests = new TempCache<>(5, TimeUnit.MINUTES);

	public DashboardSocket(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		try {
			JSONObject jo = new JSONObject(message);
			if (!jo.has("type")) return;

			String type = jo.getString("type");
			if (type.equals("login")) {
				BiContract<WebSocket, ReadyData> request = requests.computeIfAbsent(
						jo.getString("data"),
						k -> new BiContract<>((ws, data) -> {
							ws.send(data.getData().toString());
							Helper.logger(this.getClass()).debug("Handshake established with session " + jo.getString("data"));
						})
				);

				request.setSignatureA(conn);
				requests.put(jo.getString("data"), request);
				return;
			}

			JSONObject payload = jo.getJSONObject("data");
			if (!payload.has("token") || !validate(payload.getString("token"), conn)) {
				conn.send(new JSONObject() {{
					put("type", type);
					put("code", HttpURLConnection.HTTP_UNAUTHORIZED);
				}}.toString());
				return;
			}

			Token t = TokenDAO.getToken(payload.getString("token"));
			if (t == null) {
				conn.send(new JSONObject() {{
					put("type", type);
					put("code", HttpURLConnection.HTTP_UNAUTHORIZED);
				}}.toString());
				return;
			}

			switch (type) {
				case "ticket" -> {
					int number = TicketDAO.openTicket(payload.getString("message"), Main.getInfo().getUserByID(t.getUid()));
					EmbedBuilder eb = new EmbedBuilder()
							.setTitle("Feedback via site (Ticket Nº " + number + ")")
							.addField("Enviador por:", t.getHolder(), true)
							.addField("Enviado em:", Helper.dateformat.format(OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"))), true)
							.addField("Assunto", payload.getString("subject"), false)
							.addField("Mensagem:", "```" + payload.getString("message") + "```", false)
							.setFooter(t.getUid())
							.setColor(Color.decode("#fefefe"));

					Map<String, String> ids = new HashMap<>();
					for (String dev : ShiroInfo.getStaff()) {
						Main.getInfo().getUserByID(dev).openPrivateChannel()
								.flatMap(m -> m.sendMessage(eb.build()))
								.flatMap(m -> {
									ids.put(dev, m.getId());
									return m.pin();
								})
								.complete();
					}

					Main.getInfo().getUserByID(t.getUid()).openPrivateChannel()
							.flatMap(c -> c.sendMessage("**ATUALIZAÇÃO DE TICKET:** O número do seu ticket é " + number + ", você será atualizado do progresso dele."))
							.queue(null, Helper::doNothing);

					TicketDAO.setIds(number, ids);
				}
				case "validate" -> {
					User u = Main.getInfo().getUserByID(t.getUid());
					User w = Member.getWaifu(u.getId()).isBlank() ? null : Main.getInfo().getUserByID(Member.getWaifu(u.getId()));
					CoupleMultiplier cm = WaifuDAO.getMultiplier(u.getId());

					List<Member> profiles = MemberDAO.getMemberByMid(u.getId());
					JSONObject user = new JSONObject() {{
						put("waifu", w == null ? "" : w.getAsTag());
						put("waifuMult", cm == null ? 1.25f : cm.getMult());
						put("profiles", profiles.stream().map(Member::toJson).collect(Collectors.toList()));
						put("exceed", new JSONObject(ExceedDAO.getExceedState(ExceedDAO.getExceed(u.getId()))));
						put("credits", AccountDAO.getAccount(u.getId()).getBalance());
						put("bonuses", Member.getBonuses(u));
						put("badges", Tags.getUserBadges(u.getId()));
						put("rank", MemberDAO.getMemberRankPos(u.getId(), null, true));
					}};

					List<Guild> g = new ArrayList<>();
					for (Member profile : profiles) {
						Guild gd = Main.getInfo().getGuildByID(profile.getSid());
						if (gd != null) g.add(gd);
					}

					JSONArray guilds = new JSONArray();
					for (Guild gd1 : g) {
						net.dv8tion.jda.api.entities.Member mb = gd1.getMember(u);
						if (mb != null) {
							JSONObject guild = new JSONObject() {{
								put("guildID", gd1.getId());
								put("name", gd1.getName());
							}};

							guilds.put(guild);
						}
					}

					profiles.removeIf(p -> g.stream().map(Guild::getId).noneMatch(p.getSid()::equals));
					g.removeIf(gd -> profiles.stream().map(Member::getSid).noneMatch(gd.getId()::equals));
					conn.send(new JSONObject() {{
						put("type", type);
						put("code", HttpURLConnection.HTTP_OK);
						put("data", new JSONObject() {{
							put("userData", user);
							put("serverData", guilds);
						}});
					}}.toString());
				}
				case "cards" -> {
					Kawaipon kp = KawaiponDAO.getKawaipon(t.getUid());
					Set<KawaiponCard> cards = kp.getCards();
					Set<AddedAnime> animes = CardDAO.getValidAnime();

					for (AddedAnime an : animes) {
						List<JSONObject> data = new ArrayList<>();

						for (Card k : CardDAO.getCardsByAnime(an.getName())) {
							boolean normal = cards.contains(new KawaiponCard(k, false));
							boolean foil = cards.contains(new KawaiponCard(k, true));

							data.add(new JSONObject() {{
								put("id", k.getId());
								put("name", k.getName());
								put("anime", k.getAnime().getName());
								put("rarity", k.getRarity().getIndex());
								put("hasNormal", normal);
								put("hasFoil", foil);
								put("cardNormal", normal ? Helper.atob(k.drawCard(false), "png") : "");
								put("cardFoil", foil ? Helper.atob(k.drawCard(true), "png") : "");
							}});
						}

						Card ult = CardDAO.getUltimate(an.getName());
						data.add(new JSONObject() {{
							put("id", ult.getId());
							put("name", ult.getName());
							put("anime", ult.getAnime().getName());
							put("rarity", ult.getRarity().getIndex());
							put("hasNormal", CardDAO.hasCompleted(t.getUid(), an.getName(), false));
							put("hasFoil", false);
							put("cardNormal", Helper.atob(ult.drawCard(false), "png"));
							put("cardFoil", "");
						}});

						JSONObject animeCards = new JSONObject() {{
							put(an.getName(), data);
						}};

						conn.send(new JSONObject() {{
							put("type", type);
							put("code", HttpURLConnection.HTTP_OK);
							put("total", CardDAO.getValidAnime().size());
							put("data", new JSONObject() {{
								put("cardData", animeCards);
							}});
						}}.toString());
					}
				}
				case "store" -> {
					List<Pair<Object, CardType>> cards = new ArrayList<>() {{
						addAll(
								CardMarketDAO.getCardsForMarket(null, -1, -1, null, null, false, null)
										.stream()
										.map(cm -> Pair.of((Object) cm, CardType.KAWAIPON))
										.collect(Collectors.toList())
						);
						addAll(
								EquipmentMarketDAO.getCardsForMarket(null, -1, -1, -1, null)
										.stream()
										.map(em -> Pair.of((Object) em, CardType.EVOGEAR))
										.collect(Collectors.toList())
						);
						addAll(
								FieldMarketDAO.getCardsForMarket(null, -1, -1, null)
										.stream()
										.map(fm -> Pair.of((Object) fm, CardType.FIELD))
										.collect(Collectors.toList())
						);
					}};

					JSONObject data = new JSONObject();
					for (Pair<Object, CardType> card : cards) {
						switch (card.getRight()) {
							case KAWAIPON -> {
								CardMarket cm = (CardMarket) card.getLeft();
								User seller = Main.getInfo().getUserByID(cm.getSeller());
								data.put(String.valueOf(cm.getId()), new JSONObject() {{
									put("id", cm.getId());
									put("price", cm.getPrice());
									put("seller", seller == null ? "Desconhecido" : seller.getName());
									put("type", card.getRight().name().toLowerCase(Locale.ROOT));
									put("card", cm.getCard().toString());
								}});
							}
							case EVOGEAR -> {
								EquipmentMarket em = (EquipmentMarket) card.getLeft();
								User seller = Main.getInfo().getUserByID(em.getSeller());
								data.put(String.valueOf(em.getId()), new JSONObject() {{
									put("id", em.getId());
									put("price", em.getPrice());
									put("seller", seller == null ? "Desconhecido" : seller.getName());
									put("type", card.getRight().name().toLowerCase(Locale.ROOT));
									put("card", em.getCard().toString());
								}});
							}
							case FIELD -> {
								FieldMarket fm = (FieldMarket) card.getLeft();
								User seller = Main.getInfo().getUserByID(fm.getSeller());
								data.put(String.valueOf(fm.getId()), new JSONObject() {{
									put("id", fm.getId());
									put("price", fm.getPrice());
									put("seller", seller == null ? "Desconhecido" : seller.getName());
									put("type", card.getRight().name().toLowerCase(Locale.ROOT));
									put("card", fm.getCard().toString());
								}});
							}
						}
					}

					conn.send(new JSONObject() {{
						put("type", type);
						put("code", HttpURLConnection.HTTP_OK);
						put("total", cards.size());
						put("data", data);
					}}.toString());
				}
				case "card_info" -> {
					String id = payload.getString("id");
					boolean foil = payload.getBoolean("foil");
					CardType ct = payload.getEnum(CardType.class, "type");
					Account acc = AccountDAO.getAccount(t.getUid());

					JSONObject data = new JSONObject();
					switch (ct) {
						case KAWAIPON -> {
							KawaiponCard kc = new KawaiponCard(CardDAO.getCard(id), foil);
							Champion c = CardDAO.getChampion(id);
							data.put("normal", kc.getBase64());
							if (c != null) {
								c.setAcc(acc);
								data.put("alt", c.getBase64());
								data.put("info", c.toString());
							}
						}
						case EVOGEAR -> {
							Equipment e = CardDAO.getEquipment(id);
							if (e != null) {
								e.setAcc(acc);
								data.put("normal", e.getBase64());
							}
						}
						case FIELD -> {
							Field f = CardDAO.getField(id);
							if (f != null) {
								f.setAcc(acc);
								data.put("normal", f.getBase64());
							}
						}
					}

					conn.send(new JSONObject() {{
						put("type", type);
						put("code", HttpURLConnection.HTTP_OK);
						put("data", data);
					}}.toString());
				}
				case "card_buy" -> {
					Kawaipon kp = KawaiponDAO.getKawaipon(t.getUid());
					Account acc = AccountDAO.getAccount(t.getUid());

					int id = payload.getInt("id");
					boolean foil = payload.getBoolean("foil");
					CardType ct = payload.getEnum(CardType.class, "type");
					Calendar today = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT-3")));
					boolean blackfriday = today.get(Calendar.MONTH) == Calendar.NOVEMBER && today.get(Calendar.DAY_OF_MONTH) == 27;

					AtomicInteger code = new AtomicInteger(0);
					AtomicReference<String> msg = new AtomicReference<>("");
					switch (ct) {
						case KAWAIPON -> {
							CardMarket c = CardMarketDAO.getCard(id);
							if (c != null) {
								Account seller = AccountDAO.getAccount(c.getSeller());
								int rawAmount = c.getPrice();
								int liquidAmount = Helper.applyTax(acc.getUid(), rawAmount, 0.1);
								boolean taxed = rawAmount != liquidAmount;

								int err = kp.getCards().contains(c.getCard()) ? 1 : 0;
								if (err == 0) {
									if (seller.getUid().equals(t.getUid())) {
										code.set(HttpURLConnection.HTTP_OK);
										msg.set("Carta retirada com sucesso!");

										c.setBuyer(t.getUid());
										kp.addCard(c.getCard());

										KawaiponDAO.saveKawaipon(kp);
										CardMarketDAO.saveCard(c);
									} else {
										if (acc.getBalance() < rawAmount) {
											code.set(HttpURLConnection.HTTP_UNAUTHORIZED);
											msg.set("Saldo insuficiente.");
										} else {
											c.setBuyer(t.getUid());
											kp.addCard(c.getCard());
											acc.removeCredit(blackfriday ? Math.round(rawAmount * 0.75) : rawAmount, this.getClass());
											seller.addCredit(liquidAmount, this.getClass());

											LotteryValue lv = LotteryDAO.getLotteryValue();
											lv.addValue(rawAmount - liquidAmount);
											LotteryDAO.saveLotteryValue(lv);

											KawaiponDAO.saveKawaipon(kp);
											AccountDAO.saveAccount(acc);
											CardMarketDAO.saveCard(c);

											User sellerU = Main.getInfo().getUserByID(c.getSeller());
											User buyerU = Main.getInfo().getUserByID(c.getBuyer());
											if (sellerU != null) sellerU.openPrivateChannel().queue(chn -> {
														if (taxed) {
															chn.sendMessage("✅ | Sua carta `" + c.getCard().getName() + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(c.getPrice()) + " créditos!  (Taxa de venda: " + Helper.roundToString((liquidAmount * 100D / rawAmount) - 100, 1) + "%)").queue(null, Helper::doNothing);
														} else {
															chn.sendMessage("✅ | Sua carta `" + c.getCard().getName() + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(c.getPrice()) + " créditos!  (Exceed vitorioso isento de taxa)").queue(null, Helper::doNothing);
														}
													},
													Helper::doNothing
											);

											code.set(HttpURLConnection.HTTP_OK);
											msg.set("Carta comprada com sucesso!");
										}
									}
								} else {
									code.set(HttpURLConnection.HTTP_UNAUTHORIZED);
									msg.set("Você já possui essa carta.");
								}
							}
						}
						case EVOGEAR -> {
							EquipmentMarket e = EquipmentMarketDAO.getCard(id);
							if (e != null) {
								Account seller = AccountDAO.getAccount(e.getSeller());
								int rawAmount = e.getPrice();
								int liquidAmount = Helper.applyTax(acc.getUid(), rawAmount, 0.1);
								boolean taxed = rawAmount != liquidAmount;

								int err = kp.checkEquipmentError(e.getCard());
								if (err == 0) {
									if (seller.getUid().equals(t.getUid())) {
										code.set(HttpURLConnection.HTTP_OK);
										msg.set("Carta retirada com sucesso!");

										e.setBuyer(t.getUid());
										kp.addEquipment(e.getCard());

										KawaiponDAO.saveKawaipon(kp);
										EquipmentMarketDAO.saveCard(e);
									} else {
										if (acc.getBalance() < rawAmount) {
											code.set(HttpURLConnection.HTTP_UNAUTHORIZED);
											msg.set("Saldo insuficiente.");
										} else {
											e.setBuyer(t.getUid());
											kp.addEquipment(e.getCard());
											acc.removeCredit(blackfriday ? Math.round(rawAmount * 0.75) : rawAmount, this.getClass());
											seller.addCredit(liquidAmount, this.getClass());

											LotteryValue lv = LotteryDAO.getLotteryValue();
											lv.addValue(rawAmount - liquidAmount);
											LotteryDAO.saveLotteryValue(lv);

											KawaiponDAO.saveKawaipon(kp);
											AccountDAO.saveAccount(acc);
											EquipmentMarketDAO.saveCard(e);

											User sellerU = Main.getInfo().getUserByID(e.getSeller());
											User buyerU = Main.getInfo().getUserByID(e.getBuyer());
											if (sellerU != null) sellerU.openPrivateChannel().queue(chn -> {
														if (taxed) {
															chn.sendMessage("✅ | Seu equipamento `" + e.getCard().getCard().getName() + "` foi comprado por " + buyerU.getName() + " por " + Helper.separate(e.getPrice()) + " créditos!  (Taxa de venda: " + Helper.roundToString((liquidAmount * 100D / rawAmount) - 100, 1) + "%)").queue(null, Helper::doNothing);
														} else {
															chn.sendMessage("✅ | Seu equipamento `" + e.getCard().getCard().getName() + "` foi comprado por " + buyerU.getName() + " por " + Helper.separate(e.getPrice()) + " créditos!  (Exceed vitorioso isento de taxa)").queue(null, Helper::doNothing);
														}
													},
													Helper::doNothing
											);

											code.set(HttpURLConnection.HTTP_OK);
											msg.set("Carta comprada com sucesso!");
										}
									}
								} else {
									code.set(HttpURLConnection.HTTP_UNAUTHORIZED);
									switch (err) {
										case 1 -> msg.set("Você já possui " + kp.getEquipmentMaxCopies(e.getCard()) + " cópias desse evogears.");
										case 2 -> msg.set("Você não possui mais espaços para evogears tier 4.");
										case 3 -> msg.set("Você não possui mais espaços para evogears no deck.");
									}
								}
							}
						}
						case FIELD -> {
							FieldMarket f = FieldMarketDAO.getCard(id);
							if (f != null) {
								Account seller = AccountDAO.getAccount(f.getSeller());
								int rawAmount = f.getPrice();
								int liquidAmount = Helper.applyTax(acc.getUid(), rawAmount, 0.1);
								boolean taxed = rawAmount != liquidAmount;

								int err = kp.checkFieldError(f.getCard());
								if (err == 0) {
									if (seller.getUid().equals(t.getUid())) {
										code.set(HttpURLConnection.HTTP_OK);
										msg.set("Carta retirada com sucesso!");

										f.setBuyer(t.getUid());
										kp.addField(f.getCard());

										KawaiponDAO.saveKawaipon(kp);
										FieldMarketDAO.saveCard(f);
									} else {
										if (acc.getBalance() < rawAmount) {
											code.set(HttpURLConnection.HTTP_UNAUTHORIZED);
											msg.set("Saldo insuficiente.");
										} else {
											f.setBuyer(t.getUid());
											kp.addField(f.getCard());
											acc.removeCredit(blackfriday ? Math.round(rawAmount * 0.75) : rawAmount, this.getClass());
											seller.addCredit(liquidAmount, this.getClass());

											KawaiponDAO.saveKawaipon(kp);
											AccountDAO.saveAccount(acc);
											FieldMarketDAO.saveCard(f);

											User sellerU = Main.getInfo().getUserByID(f.getSeller());
											User buyerU = Main.getInfo().getUserByID(f.getBuyer());
											if (sellerU != null) sellerU.openPrivateChannel().queue(chn -> {
														if (taxed) {
															chn.sendMessage("✅ | Seu campo `" + f.getCard().getCard().getName() + "` foi comprado por " + buyerU.getName() + " por " + Helper.separate(f.getPrice()) + " créditos!  (Taxa de venda: " + Helper.roundToString((liquidAmount * 100D / rawAmount) - 100, 1) + "%)").queue(null, Helper::doNothing);
														} else {
															chn.sendMessage("✅ | Seu campo `" + f.getCard().getCard().getName() + "` foi comprado por " + buyerU.getName() + " por " + Helper.separate(f.getPrice()) + " créditos!  (Exceed vitorioso isento de taxa)").queue(null, Helper::doNothing);
														}
													},
													Helper::doNothing
											);

											code.set(HttpURLConnection.HTTP_OK);
											msg.set("Carta comprada com sucesso!");
										}
									}
								} else {
									code.set(HttpURLConnection.HTTP_UNAUTHORIZED);
									switch (err) {
										case 1 -> msg.set("Você já possui 3 cópias desse campo.");
										case 2 -> msg.set("Você não possui mais espaços para campos no deck.");
									}
								}
							}
						}
					}

					conn.send(new JSONObject() {{
						put("type", type);
						put("code", code.get());
						put("message", msg.get());
					}}.toString());
				}
				case "botstats" -> conn.send(new JSONArray(BotStatsDAO.getStats()).toString());
			}
		} catch (WebsocketNotConnectedException ignore) {
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {

	}

	@Override
	public void onStart() {
		Helper.logger(this.getClass()).info("WebSocket \"dashboard\" iniciado na porta " + this.getPort());
	}

	public void addReadyData(ReadyData rdata, String session) {
		BiContract<WebSocket, ReadyData> request = requests.computeIfAbsent(
				session,
				k -> new BiContract<>((ws, data) -> ws.send(data.getData().toString()))
		);

		request.setSignatureB(rdata);
		requests.put(session, request);
		Helper.logger(this.getClass()).debug("Received partial login request from session " + session + " (Websocket)");
	}

	public TempCache<String, BiContract<WebSocket, ReadyData>> getRequests() {
		return requests;
	}

	private boolean validate(String token, WebSocket conn) {
		if (!TokenDAO.validateToken(token)) {
			conn.send(new JSONObject() {{
				put("code", HttpURLConnection.HTTP_UNAUTHORIZED);
				put("reason", "Provided token is not valid");
			}}.toString());
			return false;
		}
		return true;
	}
}
