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

package com.kuuhaku.model.common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Anime {
	private final int idMal;
	private final String tRomaji;
	private String tEnglish;
	private final String status;
	private final String sDate;
	private final String duration;
	private final String cImage;
	private String creator;
	private String studio;
	private String naeEpisode;
	private String naeAiringAt;
	private final String description;
	private final List<Object> genres;
	private Color cColor;
	private final float score;
	private final int popularity;

	public Anime(JSONObject data) {
		JSONObject dData = data.getJSONObject("data");
		JSONObject media = dData.getJSONObject("Media");

		JSONObject cover = media.getJSONObject("coverImage");
		try {
			cColor = Color.decode(cover.getString("color"));
		} catch (Exception e) {
			cColor = Color.magenta;
		}
		cImage = cover.getString("large");

		JSONObject title = media.getJSONObject("title");
		tRomaji = title.getString("romaji");
		try {
			tEnglish = title.getString("english");
		} catch (Exception e) {
			tEnglish = tRomaji;
		}

		JSONObject date = media.getJSONObject("startDate");
		sDate = Integer.toString(date.getInt("year"));

		try {
			JSONObject staff = media.getJSONObject("staff");
			JSONArray edges = staff.getJSONArray("edges");
			JSONObject eCreator = null;
			for (int i = 0; i < edges.length(); i++) {
				if (edges.getJSONObject(i).getString("role").toLowerCase().contains("original")) {
					eCreator = edges.getJSONObject(i).getJSONObject("node").getJSONObject("name");
					break;
				} else if (edges.getJSONObject(i).getString("role").toLowerCase().contains("creator")) {
					eCreator = edges.getJSONObject(i).getJSONObject("node").getJSONObject("name");
					break;
				} else if (edges.getJSONObject(i).getString("role").toLowerCase().contains("story")) {
					eCreator = edges.getJSONObject(i).getJSONObject("node").getJSONObject("name");
					break;
				}
			}
			assert eCreator != null;
			creator = eCreator.getString("first") + " " + eCreator.getString("last");
		} catch (Exception e) {
			creator = "Não informado";
		}

		try {
			JSONObject studios = media.getJSONObject("studios");
			JSONArray sedges = studios.getJSONArray("edges");
			studio = sedges.getJSONObject(0).getJSONObject("node").getString("name");
		} catch (Exception e) {
			studio = "Não informado";
		}

		try {
			JSONObject nae = media.getJSONObject("nextAiringEpisode");
			Date naeD = new Date(nae.getLong("airingAt") * 1000);
			naeAiringAt = new SimpleDateFormat("dd/MM/yyyy").format(naeD);
		} catch (JSONException e) {
			naeAiringAt = null;
			naeEpisode = null;
		}

		idMal = media.getInt("idMal");
		status = media.getString("status").equals("FINISHED") ? "Completo" : "Transmitindo";
		String durationTemp;
		try {
			durationTemp = String.valueOf(String.valueOf(media.get("episodes")).equals("null") ? Integer.toString(media.getJSONObject("nextAiringEpisode").getInt("episode")) : media.get("episodes"));
		} catch (JSONException e) {
			durationTemp = "Desconhecido";
		}
		duration = durationTemp;
		genres = media.getJSONArray("genres").toList();
		if (media.has("averageScore")) score = media.getInt("averageScore");
		else score = -1;
		popularity = media.getInt("popularity");
		description = media.getString("description");
	}

	public int getIdMal() {
		return idMal;
	}

	public String gettRomaji() {
		return tRomaji;
	}

	public String gettEnglish() {
		return tEnglish;
	}

	public String getStatus() {
		return status;
	}

	public String getsDate() {
		return sDate;
	}

	public String getDuration() {
		return duration;
	}

	public String getcImage() {
		return cImage;
	}

	public Color getcColor() {
		return cColor;
	}

	public String getCreator() {
		return creator;
	}

	public String getStudio() {
		return studio;
	}

	public String getNaeEpisode() {
		return naeEpisode;
	}

	public String getNaeAiringAt() {
		return naeAiringAt;
	}

	public String getDescription() {
		return description;
	}

	public String getGenres() {
		return genres.toString().replace("[", "`").replace("]", "`").replace(", ", "` `");
	}

	public float getScore() {
		return score;
	}

	public int getPopularity() {
		return popularity;
	}
}
