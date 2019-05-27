package com.rdx.controller;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Tradutor {

    public static String translate(String from, String to, String text) throws IOException {
        String token = System.getenv("YANDEX_TOKEN");
        URL link = new URL("https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + token +
                "&text=" + URLEncoder.encode(text, "UTF-8") + "&lang=" + from + "-" + to);
        HttpURLConnection con = (HttpURLConnection) link.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.addRequestProperty("Accept-Charset", "UTF-8");
        System.out.println("Requisição 'GET' para o URL: " + link);
        System.out.println("Resposta: " + con.getResponseCode());

        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));

        String input;
        StringBuilder resposta = new StringBuilder();
        while ((input = br.readLine()) != null) {
            resposta.append(input);
        }
        br.close();
        con.disconnect();

        System.out.println(resposta.toString());
        JSONObject json = new JSONObject(resposta.toString());
        return json.get("text").toString().replace("[", "").replace("]", "").replace("<br>", "\n").replace("\\n", "").replace("\"", "");
    }
}
