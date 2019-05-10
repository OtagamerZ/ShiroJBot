package com.kuuhaku.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.Date;
import java.util.List;

public class Anime {
    private String tRomaji, tEnglish, status, sDate, eDate, duration, cImage, creator, studio, naeEpisode, naeAiringAt, trailer, description;
    private List<Object> genres;
    private Color cColor;
    private float score;
    private int popularity;

    public Anime(JSONObject data) {
        JSONObject cover = new JSONObject(data.getJSONObject("coverImage"));
        cColor = Color.decode(cover.getString("color"));
        cImage = cover.getString("large");

        JSONObject title = new JSONObject(data.getJSONObject("title"));
        tRomaji = title.getString("romaji");
        tEnglish = title.getString("english");

        JSONObject date = new JSONObject(data.getJSONObject("startDate"));
        sDate = date.getInt("day") + "/" + date.getInt("month") + "/" + date.getInt("year");

        JSONObject endDate = new JSONObject(data.getJSONObject("endDate"));
        eDate = endDate.getInt("day") + "/" + endDate.getInt("month") + "/" + endDate.getInt("year");

        JSONObject staff = new JSONObject(data.getJSONObject("staff"));
        JSONArray edges = new JSONArray(staff.getJSONObject("edges"));
        JSONObject eCreator = null;
        for (int i = 0; i < edges.length(); i++) {
            if (edges.getJSONObject(i).getString("role").toLowerCase().contains("creator")) {
                eCreator = edges.getJSONObject(i).getJSONObject("node").getJSONObject("name");
                break;
            }
        }
        assert eCreator != null;
        creator = eCreator.getString("last") + " " + eCreator.getString("first");

        JSONObject studios = new JSONObject(data.getJSONObject("studios"));
        JSONArray sedges = new JSONArray(studios.getJSONObject("edges"));
        studio = sedges.getJSONObject(1).getJSONObject("node").getString("name");

        JSONObject nae = new JSONObject(data.getJSONObject("nextAiringEpisode"));
        Date naeD = new Date(nae.getLong("airingAt"));
        naeAiringAt = naeD.getDay() + "/" + naeD.getMonth() + "/" + naeD.getYear();
        naeEpisode = Integer.toString(nae.getInt("episode"));

        JSONObject trailerT = new JSONObject(data.getJSONObject("trailer"));
        trailer = trailerT.getString("site");

        status = data.getString("status");
        duration = Integer.toString(data.getInt("duration"));
        genres = data.getJSONArray("genres").toList();
        score = data.getInt("averageScore");
        popularity = data.getInt("popularity");
        description = data.getString("description");

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

    public String geteDate() {
        return eDate;
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

    public String getTrailer() {
        return trailer;
    }

    public String getDescription() {
        return description;
    }

    public List<Object> getGenres() {
        return genres;
    }

    public float getScore() {
        return score;
    }

    public int getPopularity() {
        return popularity;
    }
}
