/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.utils.Helper;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Anime {
	public static String getData(String query) throws IOException {
		String json = "{\"query\":\"query" + query + "\"}";
		URL url = new URL("https://graphql.anilist.co");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setConnectTimeout(5000);
		con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		con.addRequestProperty("Accept", "application/json");
		con.addRequestProperty("User-Agent", "Mozilla/5.0");
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setRequestMethod("POST");

		OutputStream oStream = con.getOutputStream();
		oStream.write(json.getBytes(StandardCharsets.UTF_8));
		oStream.close();

		OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream());
		osw.write(Main.getInfo().getAnilistToken());
		osw.flush();

		InputStream iStream = new BufferedInputStream(con.getInputStream());
		String data = IOUtils.toString(iStream, StandardCharsets.UTF_8);
		iStream.close();

		con.disconnect();
		return data;
	}

	public static String getLink(String name) throws IOException {
		URL url = new URL("https://www.dreamanimes.com.br/anime-info/" + name);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.addRequestProperty("User-Agent", "Mozilla/5.0");
		con.setInstanceFollowRedirects(false);

		String redir = con.getHeaderField("Location");

		if (Helper.compareWithValues(con.getResponseCode(), 403, 404)) {
			return "Link indisponível";
		} else if (redir != null) {
			return getLink(redir.replace("/anime-info/", ""));
		}

		con.connect();

		return con.getURL().toString();
	}

	public static JSONObject getDAData(String name) throws IOException {
		URL url = new URL("https://www.dreamanimes.com.br/api/anime-info/" + name);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", "application/json");
		con.addRequestProperty("Accept-Charset", "UTF-8");
		con.addRequestProperty("User-Agent", "Mozilla/5.0");
		con.addRequestProperty("Authorization", System.getenv("DA_TOKEN"));
		con.setInstanceFollowRedirects(false);

		String redir = con.getHeaderField("Location");

		if (redir != null) {
			return getDAData(redir.replace("/anime-info/", ""));
		}

		JSONObject resposta = new JSONObject(IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8));

		Helper.logger(Anime.class).debug(resposta);
		return resposta.getJSONObject("anime");
	}
}
