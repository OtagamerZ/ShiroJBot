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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommonSocket extends WebSocketClient {
	private static final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private static final String TOKEN = DAO.queryNative(String.class, "SELECT token FROM access_token WHERE bearer = 'Shiro'");
	private int retry = 0;

	public CommonSocket(String address) throws URISyntaxException {
		super(new URI(address));
	}

	@Override
	public void onOpen(ServerHandshake handshake) {
		send(new JSONObject() {{
			put("type", "AUTH");
			put("token", TOKEN);
		}}.toString());
	}

	@Override
	public void onMessage(@Language("JSON5") String message) {
		JSONObject payload = new JSONObject(message);
		if (payload.isEmpty()) return;

		if (payload.getInt("code") == HttpStatus.SC_ACCEPTED) {
			if (retry > 0) {
				retry = 0;
				Constants.LOGGER.info("Reconnected to " + getClass().getSimpleName());
			} else {
				Constants.LOGGER.info("Connected to " + getClass().getSimpleName());
			}

			send(new JSONObject() {{
				put("type", "ATTACH");
				put("channels", List.of("eval"));
			}}.toString());
			return;
		}

		switch (payload.getString("channel")) {
			case "eval" -> {
				if (!payload.getString("auth").equals(DigestUtils.sha256Hex(TOKEN))) return;

				@Language("Groovy")
				String code = new String(IO.btoc(payload.getString("code")), StandardCharsets.UTF_8);

				Constants.LOGGER.info("Received eval\n" + code);
				if (payload.getString("checksum").equals(DigestUtils.md5Hex(code))) {
					Constants.LOGGER.info("Checksum pass");
					Utils.exec(code, Map.of("bot", Main.getApp().getMainShard()));
				}
			}
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		if (retry > 4) {
			Constants.LOGGER.info("Failed to reconnect to " + getClass().getSimpleName() + " in 5 retries, aborting");
			return;
		}

		if (retry > 0) {
			Constants.LOGGER.info("Failed to reconnect to " + getClass().getSimpleName() + ", retrying in " + (++retry * 5) + " seconds");
		} else {
			Constants.LOGGER.info("Disconnected from " + getClass().getSimpleName() + ", attempting reconnect in " + (++retry * 5) + " seconds");
		}

		exec.schedule(this::reconnect, retry * 5L, TimeUnit.SECONDS);
	}

	@Override
	public void onError(Exception e) {
		Constants.LOGGER.error(e, e);
	}
}
