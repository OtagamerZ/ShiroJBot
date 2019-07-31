package com.kuuhaku.controller;

import com.google.api.services.youtube.YouTube;
import com.kuuhaku.Main;
import com.kuuhaku.model.YoutubeVideo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Youtube {
	public static List<YoutubeVideo> getData(String query) throws IOException {
		URL url = new URL(YouTube.DEFAULT_BASE_URL + "search?key=" + Main.getInfo().getYoutubeToken() + "&part=snippet&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString()) + "&maxResults=5");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", "application/json");
		con.addRequestProperty("Accept-Charset", "UTF-8");
		con.addRequestProperty("User-Agent", "Mozilla/5.0");

		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));

		String input;
		StringBuilder resposta = new StringBuilder();
		while ((input = br.readLine()) != null) {
			resposta.append(input);
		}
		br.close();
		con.disconnect();

		Helper.log(Tradutor.class, LogLevel.DEBUG, resposta.toString());
		JSONObject json = new JSONObject(resposta.toString());
		JSONArray ja = json.getJSONArray("items");
		List<YoutubeVideo> videos = new ArrayList<>();
		for (Object j : ja) {
			JSONObject jid = ((JSONObject) j).getJSONObject("id");
			JSONObject jsnippet = ((JSONObject) j).getJSONObject("snippet");

			String id = jid.getString(jid.has("videoId") ? "videoId" : "playlistId");
			String title = jsnippet.getString("title");
			String desc = jsnippet.getString("description");
			String thumb = jsnippet.getJSONObject("thumbnails").getJSONObject("medium").getString("url");
			String channel = jsnippet.getString("channelTitle");
			videos.add(new YoutubeVideo(id, title, desc, thumb, channel));
		}
		return videos;
	}
}
