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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.controller.postgresql.WaifuDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.model.persistent.Tags;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.concurrent.Executors;

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
				http.setHeader("Location", "http://localhost:19006");
				http.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			}
			http.setHeader("Location", "http://localhost:19006/Loading");
			http.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);

			Executors.newSingleThreadExecutor().execute(() -> {
				try {
					user.put("token", t);
					user.put("waifu", Helper.getOr(Main.getInfo().getUserByID(Member.getWaifu(u)), ""));
					user.put("waifuMult", Helper.getOr(WaifuDAO.getMultiplier(u).getMult(), 1.25f));
					user.put("profiles", MemberDAO.getMemberByMid(u.getId()));
					user.put("exceed", ExceedDAO.getExceedState(ExceedDAO.getExceed(u.getId())));
					user.put("credits", AccountDAO.getAccount(u.getId()));
					user.put("bonuses", Member.getBonuses(u));
					user.put("badges", Tags.getUserBadges(u.getId()));

					Thread.sleep(5000);
					Main.getInfo().getServer().getSocket().getBroadcastOperations().sendEvent("auth", user.toString());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
	}
}
