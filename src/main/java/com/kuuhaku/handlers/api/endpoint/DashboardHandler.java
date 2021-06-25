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

package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.handlers.api.endpoint.payload.ReadyData;
import com.kuuhaku.handlers.api.exception.RatelimitException;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.persistent.PixelCanvas;
import com.kuuhaku.model.persistent.PixelOperation;
import com.kuuhaku.model.persistent.Token;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.User;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RestController
public class DashboardHandler {
	private final ExpiringMap<String, Boolean> ratelimit = ExpiringMap.builder().expiration(5, TimeUnit.SECONDS).build();

	@RequestMapping(value = "/auth", method = RequestMethod.GET)
	public void validateAccount(HttpServletResponse http, @RequestParam(value = "code", defaultValue = "") String code, @RequestParam(value = "error", defaultValue = "") String error) throws InterruptedException {
		if (!error.isBlank()) {
			http.setHeader("Location", ShiroInfo.SITE_ROOT);
			http.setStatus(HttpServletResponse.SC_FOUND);
			return;
		}

		JSONObject jo = new JSONObject();

		jo.put("client_id", Main.getSelfUser().getId());
		jo.put("client_secret", System.getenv("BOT_SECRET"));
		jo.put("grant_type", "authorization_code");
		jo.put("code", code);
		jo.put("redirect_uri", ShiroInfo.API_ROOT + "/auth");
		jo.put("scope", "identify");

		JSONObject token = null;

		boolean granted = false;
		while (!granted) {
			token = Helper.post("https://discord.com/api/v6/oauth2/token", Helper.urlEncode(jo), Collections.singletonMap("Content-Type", "application/x-www-form-urlencoded"), "");
			if (token.has("retry_after")) {
				Thread.sleep(token.getInt("retry_after"));
			} else granted = true;
		}

		JSONObject user = Helper.get("https://discord.com/api/v6/users/@me", new JSONObject(), Collections.emptyMap(), "Bearer " + token.getString("access_token"));
		User u = Main.getInfo().getUserByID(user.getString("id"));

		if (u != null) {
			String session = URLEncoder.encode(Helper.generateToken(u.getId(), 16), StandardCharsets.UTF_8);
			String t = TokenDAO.verifyToken(user.getString("id"));
			if (t == null) {
				http.setHeader("Location", ShiroInfo.SITE_ROOT + "/Unauthorized");
				http.setStatus(HttpServletResponse.SC_FOUND);
				return;
			}
			http.setHeader("Location", ShiroInfo.SITE_ROOT + "/Loading?s=" + session);
			http.setStatus(HttpServletResponse.SC_FOUND);

			user.put("token", t);
			Main.getInfo().getSockets().getDashboard().addReadyData(new ReadyData(user, session), session);
			Helper.logger(this.getClass()).debug("Received partial login request from session " + session + " (Discord oAuth2)");
		} else {
			http.setHeader("Location", ShiroInfo.SITE_ROOT + "/Unauthorized");
			http.setStatus(HttpServletResponse.SC_FOUND);
		}
	}

	@RequestMapping(value = "/canvas/add", method = RequestMethod.POST)
	public String addPixel(@RequestHeader(value = "token") String token, @RequestHeader(value = "pos-x") int x, @RequestHeader(value = "pos-y") int y, @RequestHeader(value = "color") String color, HttpServletResponse response) throws IllegalArgumentException {
		if (!Helper.between(x, 0, Helper.CANVAS_SIZE) || !Helper.between(y, 0, Helper.CANVAS_SIZE))
			throw new IllegalArgumentException();
		else if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		else if (ratelimit.containsKey(token)) {
			response.setHeader("Retry-After", "5");
			throw new RatelimitException();
		}

		Token t = TokenDAO.getToken(token);
		if (t == null) throw new UnauthorizedException();

		try {
			PixelOperation op = new PixelOperation(
					token,
					t.getHolder(),
					x,
					y,
					color
			);

			CanvasDAO.saveOperation(op);
		} catch (NullPointerException e) {
			throw new UnauthorizedException();
		}

		PixelCanvas.addPixel(new int[]{x, y}, Color.decode(color));

		Main.getInfo().getSockets().getCanvas().notifyUpdate(color, x, y);
		ratelimit.put(token, false);
		return new JSONObject() {{
			put("code", 200);
			put("message", "Ok! (time for next request: 5 seconds)");
			put("canvas", Main.getInfo().getCanvas().getRawCanvas());
		}}.toString();
	}

	@RequestMapping(value = "/canvas/view", method = RequestMethod.POST)
	public String viewCanvas(@RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();

		return Main.getInfo().getCanvas().getRawCanvas();
	}
}
