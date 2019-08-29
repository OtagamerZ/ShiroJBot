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

package com.kuuhaku.command.commands.Reactions;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

abstract class Reaction extends Command {
    private User user;
    private String[] reaction;
    private String[] selfTarget;

	Reaction(String name, String[] aliases, String description) {
		super(name, aliases, description, Category.FUN);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    String[] getReaction() {
        return reaction;
    }

    int getReactionLength() {
        return Helper.rng(reaction.length);
    }

    void setReaction(String[] reaction) {
        this.reaction = reaction;
    }

	String[] getSelfTarget() {
        return selfTarget;
    }

	int getSelfTargetLength() {
        return selfTarget.length;
    }

    void setSelfTarget(String[] selfTarget) {
        this.selfTarget = selfTarget;
    }

    String getUrl(String type) {
	    try {
            HttpURLConnection con = (HttpURLConnection) new URL("https://shiro-api.herokuapp.com/reaction?type=" + type).openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
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

            Helper.log(this.getClass(), LogLevel.DEBUG, resposta.toString());
            return new JSONObject(resposta.toString()).get("url").toString();
        } catch (IOException e) {
	        Helper.log(this.getClass(), LogLevel.ERROR, "Erro ao recuperar API: " + e.getStackTrace()[0]);
	        return null;
        }
    }
}
