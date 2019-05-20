/*
 * Copyright (C) 2019 Yago Garcia Sanches Gimenez / KuuHaKu
 *
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see https://www.gnu.org/licenses/
 */

package com.kuuhaku.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.util.Date;
import java.util.List;

@SuppressWarnings("deprecation")
public class Anime {
    private String tRomaji, tEnglish, status, sDate, duration, cImage, creator, studio, naeEpisode, naeAiringAt, description;
    private List<Object> genres;
    private Color cColor;
    private float score;
    private int popularity;

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
            Date naeD = new Date(nae.getLong("airingAt"));
            naeAiringAt = naeD.getDay() + "/" + naeD.getMonth() + "/" + naeD.getYear();
            naeEpisode = Integer.toString(nae.getInt("episode"));
        } catch (JSONException e) {
            naeAiringAt = null;
            naeEpisode = null;
        }

        status = media.getString("status").equals("FINISHED") ? "Completo" : "Transmitindo";
        duration = Integer.toString(media.getInt("duration"));
        genres = media.getJSONArray("genres").toList();
        score = media.getInt("averageScore");
        popularity = media.getInt("popularity");
        description = media.getString("description");
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

    public String getDuration() { return duration; }

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
