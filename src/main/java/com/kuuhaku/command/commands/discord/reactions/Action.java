/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.reactions;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.io.File;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public abstract class Action {
	private User user;
	private User[] interaction;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User[] getInteraction() {
		return interaction;
	}

	public void setInteraction(User[] interaction) {
		this.interaction = interaction;
	}

	public abstract void answer(TextChannel chn);

	public void sendReaction(String type, TextChannel channel, User target, String message, boolean allowReact) {
		Message msg = channel.sendMessage("Conectando à API...").addFile(new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("assets/loading.gif")).getPath())).complete();
		try {
			JSONObject resposta = Helper.get("https://api." + System.getenv("SERVER_URL") + "/reaction", new JSONObject() {{
				put("type", type);
			}}, null);

			Helper.logger(this.getClass()).debug(resposta);

			String url = resposta.get("url").toString();

			EmbedBuilder eb = new ColorlessEmbedBuilder();
			eb.setImage(url);
			if (allowReact) {
				eb.setFooter("↪ | Clique para retribuir");
				channel.sendMessage(message)
						.embed(eb.build())
						.queue(s -> Pages.buttonize(s, Collections.singletonMap("↪", (mb, ms) -> {
									if (mb.getId().equals(target.getId())) {
										answer(channel);
										s.clearReactions().queue();
									}
								}), false, 1, TimeUnit.MINUTES, u -> u.getId().equals(target.getId()))
						);
			} else {
				channel.sendMessage(message).embed(eb.build()).queue();
			}
		} finally {
			msg.delete().queue(null, Helper::doNothing);
		}
	}
}
