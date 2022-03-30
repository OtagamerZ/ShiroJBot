/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.FileHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.ImageHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import com.kuuhaku.utils.json.JSONObject;
import com.kuuhaku.utils.ShiroInfo;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.http.HttpStatus;

import javax.websocket.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ClientEndpoint
public class EncoderClient extends Endpoint {
	private static final ExecutorService exec = Executors.newSingleThreadExecutor();
	private static final ExpiringMap<String, CompletableFuture<String>> completed = ExpiringMap.builder().expiration(10, TimeUnit.MINUTES).build();
	private final MessageHandler.Whole<String> handler = message -> {
		JSONObject res = new JSONObject(message);

		if (res.getInt("code") == HttpStatus.OK.value()) {
			completed.get(res.getString("hash")).complete(res.getString("url"));
		} else if (res.getInt("code") == HttpStatus.REQUEST_TIMEOUT.value()) {
			completed.get(res.getString("hash")).complete(null);
		}
	};
	private Session session = null;

	public EncoderClient(String url) throws URISyntaxException, DeploymentException, IOException {
		WebSocketContainer container = ContainerProvider.getWebSocketContainer();

		ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
				.configurator(new ClientEndpointConfig.Configurator() {
					@Override
					public void beforeRequest(Map<String, List<String>> headers) {
						headers.put("Authentication", List.of(StringHelper.hash(ShiroInfo.TOKEN, "SHA-256")));
					}
				})
				.build();

		container.connectToServer(this, config, new URI(url));
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		this.session = session;
		this.session.addMessageHandler(String.class, handler);
		MiscHelper.logger(this.getClass()).debug("Conectado ao webSocket \"encoder\" com sucesso");
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		MiscHelper.logger(this.getClass()).debug("Desconectado do webSocket \"encoder\", tentando reconexão...");
		try {
			Main.getInfo().setEncoderClient(new EncoderClient(Constants.SOCKET_ROOT + "/encoder"));
		} catch (URISyntaxException | DeploymentException | IOException ignore) {
		}
	}

	public Session getSession() {
		return session;
	}

	public CompletableFuture<String> requestEncoding(String hash, List<byte[]> frames) {
		CompletableFuture<String> out = new CompletableFuture<>();
		completed.put(hash, out);

		exec.execute(() -> {
			try {
				BufferedImage bi = ImageHelper.btoa(FileHelper.uncompress(frames.get(0)));
				assert bi != null;
				send(new JSONObject() {{
					put("hash", hash);
					put("type", "BEGIN");
					put("size", frames.size());
					put("width", bi.getWidth());
					put("height", bi.getHeight());
				}}.toString());

				for (byte[] frame : frames) {
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
			} catch (IOException e) {
				MiscHelper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});

		return out;
	}

	public void send(String msg) {
		try {
			while (session == null || !session.isOpen()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ignore) {
				}
			}

			session.getBasicRemote().sendText(msg);
		} catch (IOException e) {
			MiscHelper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
