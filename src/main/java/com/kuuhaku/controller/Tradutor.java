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
import com.kuuhaku.utils.Helper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Tradutor {

    public static String translate(String from, String to, String text) throws IOException {
        String token = Main.getInfo().getYandexToken();
        URL link = new URL("https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + token +
                "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8.toString()) + "&lang=" + from + "-" + to);
        HttpURLConnection con = (HttpURLConnection) link.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.addRequestProperty("Accept-Charset", "UTF-8");

        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));

        String input;
        StringBuilder resposta = new StringBuilder();
        while ((input = br.readLine()) != null) {
            resposta.append(input);
        }
        br.close();
        con.disconnect();

        Helper.logger(Tradutor.class).debug(resposta.toString());
        JSONObject json = new JSONObject(resposta.toString());
        return json.get("text").toString().replace("[", "").replace("]", "").replace("<br>", "\n").replace("\\n", "").replace("\"", "");
    }
}
