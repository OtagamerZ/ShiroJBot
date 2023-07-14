/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.websocket;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.shoukan.Hand;
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
import com.ygimenez.json.JSONObject;
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
	private static final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
	private static final String TOKEN = DAO.queryNative(String.class, "SELECT token FROM access_token WHERE bearer = 'Shiro'");
	private int retry = 0;

	public CommonSocket(String address) throws URISyntaxException {
		super(new URI(address));
		exec.scheduleAtFixedRate(this::ping, 0, 30, TimeUnit.SECONDS);
	}

	private void ping() {
		if (isOpen()) {
			sendPing();
		}
	}

	@Override
	public void onOpen(ServerHandshake handshake) {
		send(JSONObject.of(
				Map.entry("type", "AUTH"),
				Map.entry("token", TOKEN)
		).toString());
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

				send(JSONObject.of(
						Map.entry("type", "ATTACH"),
						Map.entry("channels", List.of("eval", "shoukan", "i18n"))
				).toString());
				return;
			}

			String token = DigestUtils.sha256Hex(TOKEN);
			if (!payload.getString("auth").equals(DigestUtils.sha256Hex(TOKEN))) return;
			else if (payload.getString("channel").equals("eval")) {
				@Language("Groovy")
				String code = new String(IO.btoa(payload.getString("code")), StandardCharsets.UTF_8);

				if (payload.getString("checksum").equals(DigestUtils.md5Hex(code))) {
					Utils.exec(code, Map.of("bot", Main.getApp().getMainShard()));
				}
				return;
			}

			send(JSONObject.of(
					Map.entry("type", "ACKNOWLEDGE"),
					Map.entry("key", payload.getString("key")),
					Map.entry("token", token)
			).toString());

			MessageDigest md = DigestUtils.getDigest("md5");
			md.update(payload.getString("key").getBytes(StandardCharsets.UTF_8));
			md.update(token.getBytes(StandardCharsets.UTF_8));

			switch (payload.getString("channel")) {
				case "shoukan" -> {
					String id = payload.getString("card");
					List<CardType> types = List.copyOf(Bit.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", id)));
					Drawable<?> d = switch (types.get(types.size() - 1)) {
						case EVOGEAR -> DAO.find(Evogear.class, id);
						case FIELD -> DAO.find(Field.class, id);
						default -> DAO.find(Senshi.class, id);
					};

					Deck dk;
					if (DAO.queryNative(Integer.class, "SELECT count(1) FROM account WHERE uid = ?1", payload.getString("uid")) > 0) {
						Account acc = DAO.find(Account.class, payload.getString("uid"));
						dk = acc.getCurrentDeck();
					} else {
						dk = new Deck();
					}

					d.setHand(new Hand(payload.getString("uid"), dk));
					String b64 = IO.ctob(d.render(payload.getEnum(I18N.class, "locale"), dk), "png");
					send(JSONObject.of(
							Map.entry("type", "DELIVERY"),
							Map.entry("key", Hex.encodeHexString(md.digest())),
							Map.entry("content", b64)
					).toString());
				}
				case "i18n" -> {
					I18N locale = payload.getEnum(I18N.class, "locale");
					if (locale == null) {
						send(JSONObject.of(
								Map.entry("type", "DELIVERY"),
								Map.entry("key", Hex.encodeHexString(md.digest())),
								Map.entry("content", payload.getString("key"))
						).toString());
						return;
					}

					send(JSONObject.of(
							Map.entry("type", "DELIVERY"),
							Map.entry("key", Hex.encodeHexString(md.digest())),
							Map.entry("content", locale.get(
									payload.getString("str"),
									(Object[]) payload.getString("params").split(",")
							))
					).toString());
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
			Constants.LOGGER.info("Disconnected from " + getClass().getSimpleName() + " (" + code + "), attempting reconnect in " + (++retry * 5) + " seconds");
		}

		exec.schedule(this::reconnect, retry * 5L, TimeUnit.SECONDS);
	}

	@Override
	public void onError(Exception e) {
		Constants.LOGGER.error(e, e);
	}
}
