/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.handlers.api.endpoint.payload.ReadyData;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.jodah.expiringmap.ExpiringMap;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DashboardSocket extends WebSocketServer {
	private final ExpiringMap<String, BiContract<WebSocket, ReadyData>> requests = ExpiringMap.builder().expiration(5, TimeUnit.MINUTES).build();

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
					int number = TicketDAO.openTicket(payload.getString("message"), Main.getInfo().getMemberByID(t.getUid()));
					EmbedBuilder eb = new EmbedBuilder()
							.setTitle("Feedback via site (Ticket Nº " + number + ")")
							.addField("Enviador por:", t.getHolder(), true)
							.addField("Enviado em:", Helper.fullDateFormat.format(OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"))), true)
							.addField("Assunto", payload.getString("subject"), false)
							.addField("Mensagem:", "```" + payload.getString("message") + "```", false)
							.setFooter(t.getUid())
							.setColor(Color.decode("#fefefe"));

					Ticket tk = TicketDAO.getTicket(number);
					List<String> staff = ShiroInfo.getStaff();
					Map<String, String> ids = new HashMap<>();
					for (String dev : staff) {
						try {
							Main.getInfo().getUserByID(dev).openPrivateChannel()
									.flatMap(m -> m.sendMessageEmbeds(eb.build()))
									.flatMap(m -> {
										ids.put(dev, m.getId());
										return m.pin();
									})
									.submit().get();
						} catch (ExecutionException | InterruptedException ignore) {
						}
					}

					Main.getInfo().getUserByID(tk.getUid()).openPrivateChannel()
							.flatMap(c -> c.sendMessage("**ATUALIZAÇÃO DE TICKET:** O número do seu ticket é " + number + ", você será atualizado sobre o progresso dele."))
							.queue(null, Helper::doNothing);

					tk.setMsgIds(ids);
					TicketDAO.updateTicket(tk);
				}
				case "validate" -> {
					User u = Main.getInfo().getUserByID(t.getUid());
					User w = Member.getWaifu(u.getId()).isBlank() ? null : Main.getInfo().getUserByID(Member.getWaifu(u.getId()));
					CoupleMultiplier cm = WaifuDAO.getMultiplier(u.getId());

					List<Member> profiles = MemberDAO.getMembersByUid(u.getId());
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
					List<Market> cards = MarketDAO.getOffers(null, -1, -1, null, null, false, false, false, false, null);

					JSONObject data = new JSONObject();
					for (Market offer : cards) {
						User seller = Main.getInfo().getUserByID(offer.getSeller());
						data.put(String.valueOf(offer.getId()), new JSONObject() {{
							put("id", offer.getId());
							put("price", offer.getPrice());
							put("seller", seller == null ? "Desconhecido" : seller.getName());
							put("type", offer.getType());
							put("card", offer.getCard().toString());
						}});
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
					Deck dk = kp.getDeck();
					Account acc = AccountDAO.getAccount(t.getUid());

					int id = payload.getInt("id");
					boolean foil = payload.getBoolean("foil");
					CardType ct = payload.getEnum(CardType.class, "type");
					Calendar today = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT-3")));
					boolean blackfriday = today.get(Calendar.MONTH) == Calendar.NOVEMBER && today.get(Calendar.DAY_OF_MONTH) == 27;

					AtomicInteger code = new AtomicInteger(0);
					AtomicReference<String> msg = new AtomicReference<>("");
					Market m = MarketDAO.getCard(id);
					if (m != null) {
						Account seller = AccountDAO.getAccount(m.getSeller());
						int rawAmount = m.getPrice();
						int liquidAmount = Helper.applyTax(acc.getUid(), rawAmount, 0.1);

						int err;
						switch (m.getType()) {
							case EVOGEAR -> {
								Equipment e = m.getCard();
								err = dk.checkEquipmentError(e);
							}
							case FIELD -> {
								Field f = m.getCard();
								err = dk.checkFieldError(f);
							}
							default -> {
								KawaiponCard kc = m.getCard();
								err = kp.getCards().contains(kc) ? 1 : 0;
							}
						}

						if (err == 0) {
							if (seller.getUid().equals(t.getUid())) {
								code.set(HttpURLConnection.HTTP_OK);
								msg.set("Carta retirada com sucesso!");

								m.setBuyer(t.getUid());
								switch (m.getType()) {
									case EVOGEAR -> dk.addEquipment(m.getCard());
									case FIELD -> dk.addField(m.getCard());
									default -> kp.addCard(m.getCard());
								}

								KawaiponDAO.saveKawaipon(kp);
								MarketDAO.saveCard(m);
							} else {
								if (acc.getBalance() < rawAmount) {
									code.set(HttpURLConnection.HTTP_UNAUTHORIZED);
									msg.set("Saldo insuficiente.");
								} else {
									m.setBuyer(t.getUid());
									switch (m.getType()) {
										case EVOGEAR -> dk.addEquipment(m.getCard());
										case FIELD -> dk.addField(m.getCard());
										default -> kp.addCard(m.getCard());
									}

									acc.removeCredit(blackfriday ? Math.round(rawAmount * 0.75) : rawAmount, this.getClass());
									seller.addCredit(liquidAmount, this.getClass());

									LotteryValue lv = LotteryDAO.getLotteryValue();
									lv.addValue(rawAmount - liquidAmount);
									LotteryDAO.saveLotteryValue(lv);

									KawaiponDAO.saveKawaipon(kp);
									AccountDAO.saveAccount(acc);
									MarketDAO.saveCard(m);

									User sellerU = Main.getInfo().getUserByID(m.getSeller());
									User buyerU = Main.getInfo().getUserByID(m.getBuyer());
									String name = switch (m.getType()) {
										case EVOGEAR, FIELD -> m.getRawCard().getName();
										default -> ((KawaiponCard) m.getCard()).getName();
									};

									if (sellerU != null) sellerU.openPrivateChannel().queue(chn -> {
												boolean taxed = rawAmount != liquidAmount;
												String taxMsg = taxed ? " (Taxa: " + Helper.roundToString(100 - Helper.prcnt(liquidAmount, rawAmount) * 100, 1) + "%)" : "";
												chn.sendMessage("✅ | Sua carta `" + name + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(m.getPrice()) + " créditos!" + taxMsg).queue(null, Helper::doNothing);
											},
											Helper::doNothing
									);

									code.set(HttpURLConnection.HTTP_OK);
									msg.set("Carta comprada com sucesso!");
								}
							}
						} else {
							code.set(HttpURLConnection.HTTP_UNAUTHORIZED);
							switch (m.getType()) {
								case EVOGEAR -> {
									switch (err) {
										case 1 -> msg.set("Você já possui " + dk.getEquipmentMaxCopies(m.getCard()) + " cópias desse evogears.");
										case 2 -> msg.set("Você não possui mais espaços para evogears tier 4.");
										case 3 -> msg.set("Você não possui mais espaços para evogears no deck.");
									}
								}
								case FIELD -> {
									switch (err) {
										case 1 -> msg.set("Você já possui 3 cópias desse campo.");
										case 2 -> msg.set("Você não possui mais espaços para campos no deck.");
									}
								}
								default -> msg.set("Você já possui essa carta.");
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

	public ExpiringMap<String, BiContract<WebSocket, ReadyData>> getRequests() {
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
