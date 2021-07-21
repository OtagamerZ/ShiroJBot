/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.records.youtube.Item;
import com.kuuhaku.model.records.youtube.Snippet;
import com.kuuhaku.model.records.youtube.YoutubeData;
import com.kuuhaku.model.records.youtube.YoutubeVideo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.JSONUtils;
import com.kuuhaku.utils.ShiroInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Youtube {
	public static List<YoutubeVideo> getData(String query) throws IOException {
		JSONObject jo = Helper.get(YouTube.DEFAULT_BASE_URL, new JSONObject() {{
			put("key", ShiroInfo.getYoutubeToken());
			put("part", "snippet");
			put("type", "playlist,video");
			put("q", query);
			put("maxResults", "10");
		}});

		YoutubeData yd = JSONUtils.fromJSON(jo.toString(), YoutubeData.class);
		Helper.logger(Youtube.class).debug(yd);
		if (yd == null) return null;

		List<YoutubeVideo> videos = new ArrayList<>();

		try {
			for (Item i : yd.items()) {
				Snippet jsnippet = i.snippet();

				boolean playlist = !i.id().kind().equals("youtube#video");
				videos.add(new YoutubeVideo(
						playlist ? i.id().playlistId() : i.id().videoId(),
						jsnippet.title(),
						jsnippet.description(),
						jsnippet.thumbnails().medium().url(),
						jsnippet.channelTitle(),
						playlist
				));
			}

			return videos;
		} catch (IllegalStateException e) {
			Helper.logger(Youtube.class).error("Erro ao recuperar vídeo. Payload de dados: " + yd);
			throw new IOException();
		}
	}
}
