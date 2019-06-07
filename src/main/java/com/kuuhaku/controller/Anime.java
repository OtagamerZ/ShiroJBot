/*
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
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.controller;

import com.kuuhaku.Main;
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
        con.addRequestProperty("User-Agent", "Mozilla/5.0");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestMethod("POST");

        OutputStream oStream = con.getOutputStream();
        oStream.write(json.getBytes(StandardCharsets.UTF_8));
        oStream.close();

        OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream());
        osw.write(Main.getInfo().getAnilistToken());
        osw.flush();

        InputStream iStream = new BufferedInputStream(con.getInputStream());
        String data = IOUtils.toString(iStream, "UTF-8");
        iStream.close();

        con.disconnect();
        return data;
    }
}
