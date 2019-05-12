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
        System.out.println(cover);
        cColor = Color.decode(cover.getString("color"));
        cImage = cover.getString("large");
        System.out.println(cImage);

        JSONObject title = media.getJSONObject("title");
        tRomaji = title.getString("romaji");
        tEnglish = title.getString("english");

        JSONObject date = media.getJSONObject("startDate");
        sDate = Integer.toString(date.getInt("year"));

        JSONObject staff = media.getJSONObject("staff");
        JSONArray edges = staff.getJSONArray("edges");
        JSONObject eCreator = null;
        for (int i = 0; i < edges.length(); i++) {
            if (edges.getJSONObject(i).getString("role").toLowerCase().contains("creator")) {
                eCreator = edges.getJSONObject(i).getJSONObject("node").getJSONObject("name");
                break;
            }
        }
        assert eCreator != null;
        creator = eCreator.getString("first") + " " + eCreator.getString("last");

        JSONObject studios = media.getJSONObject("studios");
        JSONArray sedges = studios.getJSONArray("edges");
        studio = sedges.getJSONObject(0).getJSONObject("node").getString("name");

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
