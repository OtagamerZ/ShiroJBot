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

package com.kuuhaku.controller;

import com.google.api.services.youtube.YouTube;
import com.kuuhaku.model.records.youtube.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONUtils;
import com.kuuhaku.utils.ShiroInfo;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Youtube {
	private static final String API_URL = YouTube.DEFAULT_BASE_URL + "search?key=" + ShiroInfo.getYoutubeToken() + "&part=snippet&type=playlist,video&q=%s&maxResults=10";

	public static List<YoutubeVideo> getData(String query) throws IOException {
		URL url = new URL(API_URL.formatted(URLEncoder.encode(query, StandardCharsets.UTF_8.toString())));
		YoutubeData yd = requestVideoData(url);
		List<YoutubeVideo> videos = new ArrayList<>();

		try {
			for (Item i : yd.items()) {
				ID jid = i.id();
				Snippet jsnippet = i.snippet();

				videos.add(new YoutubeVideo(
						Helper.getOr(jid.videoId(), jid.playlistId()),
						jsnippet.title(),
						jsnippet.description(),
						jsnippet.thumbnails().medium().url(),
						jsnippet.channelTitle(),
						jid.playlistId() != null
				));
			}

			return videos;
		} catch (IllegalStateException e) {
			Helper.logger(Youtube.class).error("Erro ao recuperar vídeo. Payload de dados: " + yd);
			throw new IOException();
		}
	}

	private static YoutubeData requestVideoData(URL url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", "application/json");
		con.addRequestProperty("Accept-Charset", "UTF-8");
		con.addRequestProperty("User-Agent", "Mozilla/5.0");

		YoutubeData yd = JSONUtils.fromJSON(IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8), YoutubeData.class);

		Helper.logger(Youtube.class).debug(yd);
		return yd;
	}

	public static YoutubeVideo getSingleData(String query) throws IOException {
		URL url = new URL(API_URL.formatted(URLEncoder.encode(query, StandardCharsets.UTF_8.toString())));
		YoutubeData yd = requestVideoData(url);
		Item i = yd.items().get(0);

		ID jid = i.id();
		Snippet jsnippet = i.snippet();

		return new YoutubeVideo(
				Helper.getOr(jid.videoId(), jid.playlistId()),
				jsnippet.title(),
				jsnippet.description(),
				jsnippet.thumbnails().medium().url(),
				jsnippet.channelTitle(),
				jid.playlistId() != null
		);
	}
}
