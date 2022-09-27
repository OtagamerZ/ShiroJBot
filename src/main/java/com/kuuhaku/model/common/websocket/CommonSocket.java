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
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.intellij.lang.annotations.Language;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
		try {
			JSONObject payload = new JSONObject(message);
			if (payload.isEmpty()) return;

			if (payload.getString("type").equals("AUTH") && payload.getInt("code") == HttpStatus.SC_ACCEPTED) {
				if (retry > 0) {
					retry = 0;
					Constants.LOGGER.info("Reconnected to " + getClass().getSimpleName());
				} else {
					Constants.LOGGER.info("Connected to " + getClass().getSimpleName());
				}

				send(new JSONObject() {{
					put("type", "ATTACH");
					put("channels", List.of("eval", "shoukan"));
				}}.toString());
				return;
			}

			String token = DigestUtils.sha256Hex(TOKEN);
			if (!payload.getString("auth").equals(DigestUtils.sha256Hex(TOKEN))) return;

			switch (payload.getString("channel")) {
				case "eval" -> {
					@Language("Groovy")
					String code = new String(IO.btoc(payload.getString("code")), StandardCharsets.UTF_8);

					if (payload.getString("checksum").equals(DigestUtils.md5Hex(code))) {
						Utils.exec(code, Map.of("bot", Main.getApp().getMainShard()));
					}
				}
				case "shoukan" -> {
					send(new JSONObject() {{
						put("type", "ACKNOWLEDGE");
						put("key", payload.getString("key"));
						put("token", token);
					}}.toString());

					MessageDigest md = DigestUtils.getDigest("md5");
					md.update(payload.getString("key").getBytes(StandardCharsets.UTF_8));
					md.update(token.getBytes(StandardCharsets.UTF_8));

					String id = payload.getString("card");
					List<CardType> types = List.copyOf(Bit.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", id)));
					Drawable<?> d = switch (types.get(0)) {
						case EVOGEAR -> DAO.find(Evogear.class, id);
						case FIELD -> DAO.find(Field.class, id);
						default -> DAO.find(Senshi.class, id);
					};

					String b64;
					if (DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM account WHERE uid = ?1", payload.getString("uid")) > 0) {
						Account acc = DAO.find(Account.class, payload.getString("uid"));
						b64 = IO.atob(d.render(payload.getEnum(I18N.class, "locale"), acc.getCurrentDeck()), "png");
					} else {
						b64 = IO.atob(d.render(payload.getEnum(I18N.class, "locale"), new Deck()), "png");
					}

					send(new JSONObject() {{
						put("type", "DELIVERY");
						put("key", Hex.encodeHexString(md.digest()));
						put("content", b64);
					}}.toString());
				}
			}
		} catch (WebsocketNotConnectedException ignore) {
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
