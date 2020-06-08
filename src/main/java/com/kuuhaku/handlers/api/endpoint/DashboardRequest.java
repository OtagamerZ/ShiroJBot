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
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

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
			String session = URLEncoder.encode(Helper.generateToken(u.getId(), 16), StandardCharsets.UTF_8);
			String t = TokenDAO.verifyToken(user.getString("id"));
			if (t == null) {
				http.setHeader("Location", "http://" + System.getenv("SERVER_URL") + ":8200/Unauthorized");
				http.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
				return;
			}
			http.setHeader("Location", "http://" + System.getenv("SERVER_URL") + ":8200/Loading?s=" + session);
			http.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);

			user.put("token", t);
			Main.getInfo().getSockets().getDashboard().addReadyData(new ReadyData(user, session), session);
		} else {
			http.setHeader("Location", "http://" + System.getenv("SERVER_URL") + ":8200/Unauthorized");
			http.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
		}
	}

	@RequestMapping(value = "/api/card", method = RequestMethod.POST)
	public String requestCard(@RequestHeader(value = "id") String id, @RequestHeader(value = "guild") String guild) throws IOException {
		if (TokenDAO.verifyToken(id) == null) throw new UnauthorizedException();

		net.dv8tion.jda.api.entities.Member mb = Main.getInfo().getGuildByID(guild).getMemberById(id);
		assert mb != null;
		return Base64.getEncoder().encodeToString(Profile.makeProfile(mb, mb.getGuild()).toByteArray());
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
