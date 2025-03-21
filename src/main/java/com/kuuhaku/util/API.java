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

package com.kuuhaku.util;

import com.kuuhaku.Constants;
import com.ygimenez.json.JSONObject;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;
import org.java_websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class API {
	private static final Map<Class<? extends WebSocketClient>, WebSocketClient> SOCKET_CLIENTS = new HashMap<>();
	public static final CloseableHttpClient HTTP = HttpClients.custom()
			.setDefaultRequestConfig(RequestConfig.custom()
					.setCookieSpec("ignoreCookies")
					.build())
			.setDefaultHeaders(List.of(
					new BasicHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
			))
			.build();

	public static <T extends HttpUriRequestBase> JSONObject call(T req, JSONObject parameters, JSONObject headers, String body) {
		try {
			if (body != null) {
				req.setEntity(new StringEntity(body));
			}

			URIBuilder ub = new URIBuilder(req.getUri());
			if (parameters != null) {
				for (Map.Entry<String, Object> params : parameters.entrySet()) {
					ub.setParameter(params.getKey(), String.valueOf(params.getValue()));
				}
			}
			URI uri = ub.build();

			if (headers != null) {
				req.setHeaders(headers.entrySet().parallelStream()
						.map(e -> new BasicHeader(e.getKey(), String.valueOf(e.getValue())))
						.toArray(Header[]::new)
				);
			}
			req.setUri(uri);

			return HTTP.execute(req, res -> {
				HttpEntity ent = res.getEntity();

				if (ent != null) {
					String content = EntityUtils.toString(ent);
					if (!content.isBlank()) {
						return new JSONObject(EntityUtils.toString(ent));
					} else {
						return new JSONObject();
					}
				} else {
					return new JSONObject();
				}
			});
		} catch (IOException | URISyntaxException e) {
			return new JSONObject();
		}
	}

	public static void connectSocket(Class<? extends WebSocketClient> client, String address) {
		try {
			WebSocketClient wc = client.getDeclaredConstructor(String.class).newInstance(address);
			wc.connectBlocking(1, TimeUnit.MINUTES);
			SOCKET_CLIENTS.put(client, wc);
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to connect to socket at {}", address, e);
		}
	}

	public static WebSocketClient getSocket(Class<? extends WebSocketClient> client) {
		return SOCKET_CLIENTS.get(client);
	}
}
