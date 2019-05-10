package com.kuuhaku.commands;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class Anime {
    static String getData(String query) throws IOException {
        String json = "{'query':'query " + query + "'}";
        URL url = new URL("https://graphql.anilist.co");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(5000);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.addRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestMethod("POST");

        OutputStream oStream = con.getOutputStream();
        oStream.write(json.getBytes("UTF-8"));
        oStream.close();

        InputStream iStream = new BufferedInputStream(con.getInputStream());
        String data = IOUtils.toString(iStream, "UTF-8");
        iStream.close();

        con.disconnect();
        return data;
    }
}
