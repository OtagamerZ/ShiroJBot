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
import org.springframework.http.HttpStatus;

import javax.websocket.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ClientEndpoint
public class EncoderClient {
	private static final ExecutorService exec = Executors.newSingleThreadExecutor();
	private static final TempCache<String, CompletableFuture<String>> completed = new TempCache<>(10, TimeUnit.MINUTES);
	private Session session = null;

	public EncoderClient(String url) throws URISyntaxException, DeploymentException, IOException {
		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		container.connectToServer(this, new URI(url));
	}

	@OnOpen
	public void onOpen(Session session) {
		this.session = session;
		Helper.logger(this.getClass()).info("Conectado ao webSocket \"encoder\" com sucesso");
	}

	@OnMessage
	public void onMessage(String message) {
		JSONObject res = new JSONObject(message);

		if (res.getInt("code") == HttpStatus.OK.value()) {
			completed.get(res.getString("hash")).complete(res.getString("url"));
		}
	}

	@OnClose
	public void onClose(Session session, CloseReason reason) {
		Helper.logger(this.getClass()).info("Desconectado do webSocket \"encoder\", tentando reconexão...");
		try {
			Main.getInfo().setEncoderClient(new EncoderClient(ShiroInfo.SOCKET_ROOT + "/encoder"));
		} catch (URISyntaxException | DeploymentException | IOException ignore) {
		}
	}

	public Session getSession() {
		return session;
	}

	public CompletableFuture<String> requestEncoding(String hash, List<String> frames) {
		CompletableFuture<String> out = new CompletableFuture<>();
		completed.put(hash, out);

		exec.execute(() -> {
			BufferedImage bi = Helper.btoa(frames.get(0));
			assert bi != null;
			send(new JSONObject() {{
				put("hash", hash);
				put("type", "BEGIN");
				put("size", frames.size());
				put("width", bi.getWidth());
				put("heigth", bi.getHeight());
			}}.toString());

			for (String frame : frames) {
				send(new JSONObject() {{
					put("hash", hash);
					put("type", "NEXT");
					put("data", frame);
				}}.toString());
			}

			send(new JSONObject() {{
				put("hash", hash);
				put("type", "END");
			}}.toString());
		});

		return out;
	}

	public void send(String msg) {
		try {
			session.getBasicRemote().sendText(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
