package com.rdx.controller;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Anime {
    public static String getData(String query) throws IOException {
        String json = "{\"query\":\"query" + query + "\"}";
        System.out.println(json);
        URL url = new URL("https://graphql.anilist.co");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(5000);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.addRequestProperty("Accept", "application/json");
        con.addRequestProperty("User-Agent", "OtagamerZ");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestMethod("POST");

        OutputStream oStream = con.getOutputStream();
        oStream.write(json.getBytes(StandardCharsets.UTF_8));
        oStream.close();

        OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream());
        osw.write("client_credentials&Client_id=1987&Client_secret=oRIwidZip6e1mVHNFu5IA3gvfBwfCGdTxyC0tTGb");
        osw.flush();

        InputStream iStream = new BufferedInputStream(con.getInputStream());
        String data = IOUtils.toString(iStream, "UTF-8");
        iStream.close();

        con.disconnect();
        return data;
    }
}
