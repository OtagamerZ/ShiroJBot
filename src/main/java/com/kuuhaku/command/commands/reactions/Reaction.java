/*
 * This file is part of Shiro J Bot.
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.reactions;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class Reaction extends Command {
	private User user;
	private String[] reaction;
	private String[] selfTarget;
	private final boolean answerable;
	private final String type;
	private User[] interaction;

	public Reaction(String name, String[] aliases, String description, boolean answerable, String type) {
		super(name, aliases, description, Category.FUN);
		this.answerable = answerable;
		this.type = type;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getReaction() {
		return reaction.length == 0 ? "SEM RESPOSTA SETADA" : reaction[Helper.rng(reaction.length)];
	}

	public boolean isAnswerable() {
		return answerable;
	}

	public String getType() {
		return type;
	}

	public void setReaction(String[] reaction) {
		this.reaction = reaction;
	}

	public String getSelfTarget() {
		return selfTarget.length == 0 ? "SEM RESPOSTA SETADA" : selfTarget[Helper.rng(selfTarget.length)];
	}

	public void setSelfTarget(String[] selfTarget) {
		this.selfTarget = selfTarget;
	}

	public User[] getInteraction() {
		return interaction;
	}

	public void setInteraction(User[] interaction) {
		this.interaction = interaction;
	}

	public abstract void answer(TextChannel chn);

	public void sendReaction(String type, TextChannel chn, String message, boolean allowReact) {
		Message msg = chn.sendMessage("Conectando à API...").addFile(new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("loading.gif")).getPath())).complete();
		try {
			HttpURLConnection con = (HttpURLConnection) new URL("https://shiro-api.herokuapp.com/reaction?type=" + type).openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestMethod("GET");
			con.setRequestProperty("Accept", "application/json");
			con.addRequestProperty("Accept-Charset", "UTF-8");

			JSONObject resposta = new JSONObject(IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8));

			Helper.logger(this.getClass()).debug(resposta);

			String url = resposta.get("url").toString();

			EmbedBuilder eb = new EmbedBuilder();
			eb.setImage(url);

			Helper.sendReaction(this, url, chn, allowReact).accept(chn.sendMessage(message).embed(eb.build()));
		} catch (IOException e) {
			Helper.logger(this.getClass()).error("Erro ao recuperar API: " + e.getStackTrace()[0]);
		} catch (IllegalAccessException ignore) {
		} finally {
			msg.delete().queue();
		}
	}
}
