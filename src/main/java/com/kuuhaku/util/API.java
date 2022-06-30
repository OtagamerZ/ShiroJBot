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

package com.kuuhaku.util;

import com.kuuhaku.Constants;
import com.kuuhaku.util.json.JSONObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.java_websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class API {
	private static final Map<Class<? extends WebSocketClient>, WebSocketClient> socketClients = new HashMap<>();
	private static final CloseableHttpClient HTTP = HttpClients.custom().setDefaultHeaders(List.of(
			new BasicHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
	)).build();

	public static <T extends HttpEntityEnclosingRequestBase> JSONObject call(T req, JSONObject parameters, JSONObject headers, String body) {
		try {
			req.setEntity(new StringEntity(body));

			URIBuilder ub = new URIBuilder(req.getURI());
			for (Map.Entry<String, Object> params : parameters.entrySet()) {
				ub.setParameter(params.getKey(), String.valueOf(params.getValue()));
			}
			URI uri = ub.build();

			req.setHeaders(headers.entrySet().parallelStream()
					.map(e -> new BasicHeader(e.getKey(), String.valueOf(e.getValue())))
					.toArray(Header[]::new)
			);
			req.setURI(uri);

			try (CloseableHttpResponse res = HTTP.execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null) {
					return new JSONObject(EntityUtils.toString(ent));
				} else {
					return new JSONObject();
				}
			}
		} catch (IOException | URISyntaxException e) {
			return new JSONObject();
		}
	}

	public static void connectSocket(Class<? extends WebSocketClient> client, String address) {
		try {
			WebSocketClient wc = client.getDeclaredConstructor(String.class).newInstance(address);
			wc.connectBlocking(1, TimeUnit.MINUTES);
			socketClients.put(client, wc);
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to connect to socket at " + address, e);
		}
	}
}
