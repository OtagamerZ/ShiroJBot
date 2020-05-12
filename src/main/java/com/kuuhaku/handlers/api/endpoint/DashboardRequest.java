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

package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.common.ExportableGuildConfig;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
public class DashboardRequest {

	@RequestMapping(value = "/api/auth", method = RequestMethod.GET)
	public void validateAccount(HttpServletResponse http, @RequestParam(value = "code") String code) throws InterruptedException {
		JSONObject jo = new JSONObject();

		jo.put("client_id", Main.getInfo().getSelfUser().getId());
		jo.put("client_secret", System.getenv("BOT_SECRET"));
		jo.put("grant_type", "authorization_code");
		jo.put("code", code);
		jo.put("redirect_uri", "http://" + System.getenv("SERVER_URL") + "/api/auth");
		jo.put("scope", "identify");


		JSONObject token = null;

		boolean granted = false;
		while (!granted) {
			token = Helper.post("https://discord.com/api/v6/oauth2/token", Helper.urlEncode(jo), Collections.singletonMap("Content-Type", "application/x-www-form-urlencoded"), "");
			if (token.has("retry_after")) {
				Thread.sleep(token.getInt("retry_after"));
			} else granted = true;
		}

		JSONObject user = Helper.get("https://discord.com/api/v6/users/@me", new JSONObject(), Collections.emptyMap(), token.getString("token_type") + " " + token.getString("access_token"));
		User u = Main.getInfo().getUserByID(user.getString("id"));

		if (u != null) {
			String t = TokenDAO.verifyToken(user.getString("id"));
			if (t == null) {
				http.setHeader("Location", "http://" + System.getenv("SERVER_URL") + ":19006");
				http.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
				Main.getInfo().getServer().getSocket().getBroadcastOperations().sendEvent("auth", "unauthorized");
				return;
			}
			http.setHeader("Location", "http://" + System.getenv("SERVER_URL") + ":19006/Loading");
			http.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			String w = Member.getWaifu(u);
			CoupleMultiplier cm = WaifuDAO.getMultiplier(u);

			Executors.newSingleThreadExecutor().execute(() -> {
				List<Member> profiles = MemberDAO.getMemberByMid(u.getId());

				user.put("token", t);
				user.put("waifu", w.isEmpty() ? "" : Helper.getOr(Main.getInfo().getUserByID(w), ""));
				user.put("waifuMult", cm == null ? 1.25f : cm.getMult());
				user.put("profiles", profiles);
				user.put("exceed", new JSONObject(ExceedDAO.getExceedState(ExceedDAO.getExceed(u.getId()))));
				user.put("credits", AccountDAO.getAccount(u.getId()).getBalance());
				user.put("bonuses", Member.getBonuses(u));
				user.put("badges", Tags.getUserBadges(u.getId()));

				Main.getInfo().getServer().getSocket().getBroadcastOperations().sendEvent("auth_user", user.toString());

				List<Guild> g = u.getMutualGuilds();

				JSONArray guilds = new JSONArray();
				g.forEach(gd -> {
					JSONObject guild = new JSONObject();

					guild.put("guildID", gd.getId());
					guild.put("name", gd.getName());
					guild.put("moderator", Helper.hasPermission(gd.getMember(u), PrivilegeLevel.MOD));
					guild.put("channels", gd.getTextChannels().stream().map(tc -> new JSONObject() {{
						put("id", tc.getId());
						put("name", tc.getName());
					}}).collect(Collectors.toList()));
					guild.put("roles", gd.getRoles().stream().map(r -> new JSONObject() {{
						put("id", r.getId());
						put("name", r.getName());
					}}).collect(Collectors.toList()));
					guild.put("configs", new ExportableGuildConfig(GuildDAO.getGuildById(gd.getId())).getGuildConfig());

					guilds.put(guild);
				});

				Main.getInfo().getServer().getSocket().getBroadcastOperations().sendEvent("auth", "authorized");
				Main.getInfo().getServer().getSocket().getBroadcastOperations().sendEvent("auth_guild", guilds.toString());
			});
		} else {
			http.setHeader("Location", "http://" + System.getenv("SERVER_URL") + ":19006");
			http.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			Main.getInfo().getServer().getSocket().getBroadcastOperations().sendEvent("auth", "unauthorized");
		}
	}

	@RequestMapping(value = "/api/card", method = RequestMethod.POST)
	public String requestCard(@RequestHeader(value = "id") String id, @RequestHeader(value = "guild") String guild) throws IOException {
		if (TokenDAO.verifyToken(id) == null) throw new UnauthorizedException();

		net.dv8tion.jda.api.entities.Member mb = Main.getInfo().getGuildByID(guild).getMemberById(id);
		assert mb != null;
		return Base64.getEncoder().encodeToString(Profile.makeProfile(mb, mb.getGuild()).toByteArray());
	}

	@RequestMapping(value = "/api/update", method = RequestMethod.POST)
	public void updateData(@RequestHeader(value = "token") String token, @RequestBody String payload) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		JSONObject cluster = new JSONObject(payload);

		if (cluster.has("guildData")) {
			JSONObject guild = cluster.getJSONObject("guildData");

			GuildConfig gc = GuildDAO.getGuildById(guild.getString("guildID"));

			JSONObject c = guild.getJSONObject("configs");

			gc.setPrefix(c.getString("prefix"));

			gc.setWarnTime(c.getInt("muteTime"));
			gc.setPollTime(c.getInt("pollTime"));

			if (!c.getJSONObject("muteRole").isEmpty()) gc.setCargoWarn(c.getJSONObject("muteRole").getString("id"));

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

		if (cluster.has("profileData")) {
			JSONObject pd = cluster.getJSONObject("profileData");

			JSONObject data = pd;
			Member mb = MemberDAO.getMemberById(data.getString("id"));

			mb.setBg(data.getString("bg"));
			mb.setBio(data.getString("bio"));

			MemberDAO.updateMemberConfigs(mb);
		}
	}

	@RequestMapping(value = "/api/ticket", method = RequestMethod.POST)
	public void sendTicket(@RequestHeader(value = "token") String token, @RequestBody String payload) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		JSONObject data = new JSONObject(payload);

		Token t = TokenDAO.getToken(token);

		if (t == null) return;

		int number = TicketDAO.openTicket(data.getString("message"), Main.getInfo().getUserByID(t.getUid()));

		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Feedback via site (Ticket Nº " + number + ")");
		eb.addField("Enviador por:", t.getHolder(), true);
		eb.addField("Enviado em:", Helper.dateformat.format(OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"))), true);
		eb.addField("Assunto", data.getString("subject"), false);
		eb.addField("Mensagem:", "```" + data.getString("message") + "```", false);
		eb.setColor(Color.decode("#fefefe"));

		Map<String, String> ids = new HashMap<>();

		Main.getInfo().getDevelopers().forEach(dev -> Main.getInfo().getUserByID(dev).openPrivateChannel()
				.flatMap(m -> m.sendMessage(eb.build()))
				.flatMap(m -> {
					ids.put(dev, m.getId());
					return m.pin();
				})
				.complete()
		);

		TicketDAO.setIds(number, ids);
	}

	@RequestMapping(value = "/api/checkImage", method = RequestMethod.POST)
	public String checkImage(@RequestHeader(value = "imageUrl") String url) {
		try {
			ImageIO.read(new URL(url));
			return new JSONObject().put("valid", true).toString();
		} catch (IOException e) {
			return new JSONObject().put("valid", false).toString();
		}
	}
}
