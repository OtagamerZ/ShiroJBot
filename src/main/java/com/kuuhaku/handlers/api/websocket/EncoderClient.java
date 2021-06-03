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

import com.kuuhaku.Main;
import com.kuuhaku.model.common.TempCache;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.ShiroInfo;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.http.HttpStatus;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EncoderClient extends WebSocketClient {
	private static final ExecutorService exec = Executors.newSingleThreadExecutor();
	private static final TempCache<String, CompletableFuture<String>> completed = new TempCache<>(10, TimeUnit.MINUTES);

	public EncoderClient(String serverUri) throws URISyntaxException {
		super(new URI(serverUri));
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		Helper.logger(this.getClass()).info("Conectado ao webSocket \"encoder\" com sucesso");
	}

	@Override
	public void onMessage(String message) {
		JSONObject res = new JSONObject(message);

		if (res.getInt("code") == HttpStatus.OK.value()) {
			completed.get(res.getString("hash")).complete(res.getString("url"));
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		Helper.logger(this.getClass()).info("Desconectado do webSocket \"encoder\", tentando reconexão...");
		try {
			if (reconnectBlocking()) {
				Helper.logger(this.getClass()).info("Reconectado ao webSocket \"encoder\" com sucesso");
			} else {
				Main.getInfo().setEncoderClient(new EncoderClient(ShiroInfo.SOCKET_ROOT + "/encoder"));
			}
		} catch (InterruptedException | URISyntaxException ignore) {
		}
	}

	@Override
	public void onError(Exception ex) {

	}

	public CompletableFuture<String> requestEncoding(String hash, List<String> frames) {
		CompletableFuture<String> out = new CompletableFuture<>();
		completed.put(hash, out);

		exec.execute(() -> {
			BufferedImage bi = Helper.btoa(frames.get(0));
			assert bi != null;
			getConnection().send(new JSONObject() {{
				put("hash", hash);
				put("type", "BEGIN");
				put("size", frames.size());
				put("width", bi.getWidth());
				put("heigth", bi.getHeight());
			}}.toString());

			for (String frame : frames) {
				getConnection().send(new JSONObject() {{
					put("hash", hash);
					put("type", "NEXT");
					put("data", frame);
				}}.toString());
			}

			getConnection().send(new JSONObject() {{
				put("hash", hash);
				put("type", "END");
			}}.toString());
		});

		return out;
	}
}
