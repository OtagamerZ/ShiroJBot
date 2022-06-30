/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.websocket;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.intellij.lang.annotations.Language;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CommonSocket extends WebSocketClient {
	public CommonSocket(String address) throws URISyntaxException {
		super(new URI(address));
	}

	@Override
	public void onOpen(ServerHandshake handshake) {
		send(new JSONObject() {{
			put("type", "AUTH");
			put("token", DAO.queryNative(String.class, "SELECT token FROM access_token WHERE bearer = 'Shiro'"));
		}}.toString());
	}

	@Override
	public void onMessage(@Language("JSON5") String message) {
		JSONObject payload = new JSONObject(message);
		if (payload.isEmpty()) return;

		if (payload.getInt("code") == HttpStatus.SC_ACCEPTED) {
			Constants.LOGGER.info("Connected to socket " + getClass().getSimpleName());
			return;
		}

		switch (payload.getString("channel")) {
			case "eval" -> {
				@Language("Groovy")
				String code = new String(IO.btoc(payload.getString("code")), StandardCharsets.UTF_8);

				if (code.equals(DigestUtils.md5Hex(payload.getString("checksum")))) {
					Utils.exec(code, Map.of("bot", Main.getApp().getMainShard()));
				}
			}
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		Constants.LOGGER.info("Disconnected from socket " + getClass().getSimpleName());
	}

	@Override
	public void onError(Exception e) {
		Constants.LOGGER.error(e, e);
	}
}
