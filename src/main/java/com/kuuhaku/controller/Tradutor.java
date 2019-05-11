package com.kuuhaku.controller;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Tradutor {
    public static String translate(String from, String to, String text) throws IOException {
        String urlStr = "https://script.google.com/macros/s/AKfycbxZyU7WNaXJGaI7YgQPaqpUFRuGgLMVVBi_g5MbrbYS/exec" +
                "?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8.toString()) +
                "&target=" + to +
                "&source=" + from;
        System.out.println(urlStr);
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String input;
        while ((input = in.readLine()) != null) {
            response.append(input);
        }

        return response.toString();
    }
}
