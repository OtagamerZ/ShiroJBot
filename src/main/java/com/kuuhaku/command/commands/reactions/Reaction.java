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

package com.kuuhaku.command.commands.reactions;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

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
        return Helper.rng(selfTarget.length);
    }

    void setSelfTarget(String[] selfTarget) {
        this.selfTarget = selfTarget;
    }

    String getUrl(String type, TextChannel chn) {
        AtomicReference<Message> msg = new AtomicReference<>();
        chn.sendMessage("Conectando à API...").addFile(new File(Objects.requireNonNull(Helper.class.getClassLoader().getResource("loading.gif")).getPath())).queue(msg::set);
	    try {
            HttpURLConnection con = (HttpURLConnection) new URL("https://shiro-api.herokuapp.com/reaction?type=" + type).openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            con.addRequestProperty("Accept-Charset", "UTF-8");

            String resposta = Helper.getResponse(con);

            Helper.logger(this.getClass()).debug(resposta);
            return new JSONObject(resposta).get("url").toString();
        } catch (IOException e) {
	        Helper.logger(this.getClass()).error("Erro ao recuperar API: " + e.getStackTrace()[0]);
	        return null;
        } finally {
            msg.get().delete().queue();
        }
    }
}
