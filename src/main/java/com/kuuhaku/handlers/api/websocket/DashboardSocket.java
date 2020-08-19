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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.api.endpoint.ReadyData;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.AnimeName;
import com.kuuhaku.utils.BiContract;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
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
import java.util.stream.Collectors;

public class DashboardSocket extends WebSocketServer {
	private final Cache<String, BiContract<WebSocket, ReadyData>> requests = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

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

			if (jo.getString("type").equals("login")) {
				BiContract<WebSocket, ReadyData> request = requests.getIfPresent(jo.getString("data"));
				if (request == null) request = new BiContract<>((ws, data) -> ws.send(data.getData().toString()));
				request.setSignatureA(conn);
				requests.put(jo.getString("data"), request);
				return;
			}

			JSONObject payload = jo.getJSONObject("data");
			if (!payload.has("token") || !validate(payload.getString("token"), conn)) {
				conn.send(new JSONObject() {{
					put("type", jo.getString("type"));
					put("code", HttpURLConnection.HTTP_UNAUTHORIZED);
				}}.toString());
				return;
			}
			Token t = TokenDAO.getToken(payload.getString("token"));
			if (t == null) {
				conn.send(new JSONObject() {{
					put("type", jo.getString("type"));
					put("code", HttpURLConnection.HTTP_UNAUTHORIZED);
				}}.toString());
				return;
			}

			switch (jo.getString("type")) {
				case "update":
					if (payload.has("guildData")) {
						JSONObject guild = payload.getJSONObject("guildData");

						GuildConfig gc = GuildDAO.getGuildById(guild.getString("guildID"));

						JSONObject c = guild.getJSONObject("configs");

						gc.setPrefix(c.getString("prefix"));

						gc.setWarnTime(c.getInt("muteTime"));
						gc.setPollTime(c.getInt("pollTime"));

						if (!c.getJSONObject("muteRole").isEmpty())
							gc.setCargoWarn(c.getJSONObject("muteRole").getString("id"));

						gc.setMsgBoasVindas(c.getString("welcomeMessage"));
						gc.setMsgAdeus(c.getString("goodbyeMessage"));

						if (!c.getJSONObject("welcomeChannel").isEmpty())
							gc.setCanalBV(c.getJSONObject("welcomeChannel").getString("id"));
						if (!c.getJSONObject("goodbyeChannel").isEmpty())
							gc.setCanalAdeus(c.getJSONObject("goodbyeChannel").getString("id"));
						if (!c.getJSONObject("suggestionChannel").isEmpty())
							gc.setCanalSUG(c.getJSONObject("suggestionChannel").getString("id"));
						if (!c.getJSONObject("relayChannel").isEmpty())
							gc.setCanalRelay(c.getJSONObject("relayChannel").getString("id"));
						if (!c.getJSONObject("levelUpChannel").isEmpty())
							gc.setCanalLvl(c.getJSONObject("levelUpChannel").getString("id"));

						JSONObject lr = new JSONObject();
						c.getJSONArray("levelRoles").forEach(o -> lr.put(((JSONObject) o).getString("level"), ((JSONObject) o).getString("id")));

						gc.setCargosLvl(lr);

						GuildDAO.updateGuildSettings(gc);
					}

					if (payload.has("profileData")) {
						JSONObject data = payload.getJSONObject("profileData");
						Member mb = MemberDAO.getMemberById(data.getString("id"));

						mb.setBg(data.getString("bg"));
						mb.setBio(data.getString("bio"));

						MemberDAO.updateMemberConfigs(mb);
					}
					break;
				case "ticket":
					int number = TicketDAO.openTicket(payload.getString("message"), Main.getInfo().getUserByID(t.getUid()));

					EmbedBuilder eb = new EmbedBuilder();

					eb.setTitle("Feedback via site (Ticket Nº " + number + ")");
					eb.addField("Enviador por:", t.getHolder(), true);
					eb.addField("Enviado em:", Helper.dateformat.format(OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"))), true);
					eb.addField("Assunto", payload.getString("subject"), false);
					eb.addField("Mensagem:", "```" + payload.getString("message") + "```", false);
					eb.setColor(Color.decode("#fefefe"));

					Map<String, String> ids = new HashMap<>();

					ShiroInfo.getStaff().forEach(dev -> Main.getInfo().getUserByID(dev).openPrivateChannel()
							.flatMap(m -> m.sendMessage(eb.build()))
							.flatMap(m -> {
								ids.put(dev, m.getId());
								return m.pin();
							})
							.complete()
					);

					TicketDAO.setIds(number, ids);
					break;
				case "validate":
					User u = Main.getInfo().getUserByID(t.getUid());
					User w = Member.getWaifu(u).isBlank() ? null : Main.getInfo().getUserByID(Member.getWaifu(u));
					CoupleMultiplier cm = WaifuDAO.getMultiplier(u);

					List<Member> profiles = MemberDAO.getMemberByMid(u.getId());
					JSONObject user = new JSONObject() {{
						put("waifu", w == null ? "" : w.getAsTag());
						put("waifuMult", cm == null ? 1.25f : cm.getMult());
						put("profiles", profiles.stream().map(Member::toJson).collect(Collectors.toList()));
						put("exceed", new JSONObject(ExceedDAO.getExceedState(ExceedDAO.getExceed(u.getId()))));
						put("credits", AccountDAO.getAccount(u.getId()).getBalance());
						put("bonuses", Member.getBonuses(u));
						put("badges", Tags.getUserBadges(u.getId()));
					}};

					List<Guild> g = new ArrayList<>();
					profiles.forEach(p -> {
						Guild gd = Main.getInfo().getGuildByID(p.getSid());
						if (gd != null) g.add(gd);
					});

					JSONArray guilds = new JSONArray();
					g.forEach(gd -> {
						net.dv8tion.jda.api.entities.Member mb = gd.getMember(u);
						if (mb != null) {
							JSONObject guild = new JSONObject() {{
								put("guildID", gd.getId());
								put("name", gd.getName());
							}};

							guilds.put(guild);
						}
					});

					Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
					List<JSONObject> data = new ArrayList<>();
					Set<KawaiponCard> cards = kp.getCards();
					for (AnimeName anime : AnimeName.values()) {
						if (CardDAO.totalCards(anime) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime) && !k.isFoil()).count())
							cards.add(new KawaiponCard(CardDAO.getUltimate(anime), false));
					}

					cards.forEach(k -> data.add(new JSONObject() {{
						put("id", k.getCard().getId());
						put("anime", k.getCard().getAnime().toString());
						put("rarity", k.getCard().getRarity().getIndex());
						put("foil", k.isFoil());
						put("card", Base64.getEncoder().encode(Helper.getBytes(k.getCard().drawCard(k.isFoil()))));
					}}));

					JSONObject cardData = new JSONObject() {{
						put("animes", List.of(AnimeName.values()));
						put("cards", data);
					}};

					profiles.removeIf(p -> g.stream().map(Guild::getId).noneMatch(p.getSid()::equals));
					g.removeIf(gd -> profiles.stream().map(Member::getSid).noneMatch(gd.getId()::equals));

					conn.send(new JSONObject() {{
						put("type", "validate");
						put("code", HttpURLConnection.HTTP_OK);
						put("data", new JSONObject() {{
							put("userData", user);
							put("serverData", guilds);
							put("cardData", cardData);
						}});
					}}.toString());
					break;
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
		BiContract<WebSocket, ReadyData> request = requests.getIfPresent(session);
		if (request == null) request = new BiContract<>((ws, data) -> ws.send(data.getData().toString()));
		request.setSignatureB(rdata);
		requests.put(session, request);
	}

	public Cache<String, BiContract<WebSocket, ReadyData>> getRequests() {
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
