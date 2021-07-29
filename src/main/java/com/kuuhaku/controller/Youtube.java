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

package com.kuuhaku.controller;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import com.kuuhaku.model.records.YoutubeVideo;
import com.kuuhaku.utils.ShiroInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Youtube {
	private static final YouTube yt = new YouTube.Builder(
			new NetHttpTransport(),
			new GsonFactory(),
			request -> {
			}
	).setApplicationName(ShiroInfo.getName()).build();

	public static List<YoutubeVideo> getData(String query) throws IOException {
		SearchListResponse search = yt.search().list(List.of("snippet"))
				.setKey(ShiroInfo.getYoutubeToken())
				.setQ(query)
				.setType(List.of("playlist", "video"))
				.setMaxResults(10L)
				.execute();

		List<YoutubeVideo> videos = new ArrayList<>();
		for (SearchResult i : search.getItems()) {
			SearchResultSnippet s = i.getSnippet();

			boolean playlist = i.getId().getKind().equals("youtube#playlist");
			videos.add(new YoutubeVideo(
					playlist ? i.getId().getPlaylistId() : i.getId().getVideoId(),
					s.getTitle(),
					s.getDescription(),
					s.getThumbnails().getDefault().getUrl(),
					s.getChannelTitle(),
					playlist
			));
		}

		return videos;
	}
}
