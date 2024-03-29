/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.Main;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class Action {
	private String user;
	private String[] interaction;

	public User getUser() {
		return Main.getInfo().getUserByID(user);
	}

	public void setUser(User user) {
		this.user = user.getId();
	}

	public User[] getInteraction() {
		return Arrays.stream(interaction)
				.map(Main.getInfo()::getUserByID)
				.toArray(User[]::new);
	}

	public void setInteraction(User[] interaction) {
		this.interaction = Arrays.stream(interaction)
				.map(User::getId)
				.toArray(String[]::new);
	}

	public abstract void answer(TextChannel chn);

	public void sendReaction(String type, TextChannel channel, User target, String message, boolean allowReact) {
		File loading = Helper.getResourceAsFile(this.getClass(), "assets/loading.gif");
		assert loading != null;

		channel.sendMessage("Conectando à API...")
				.addFile(loading)
				.queue(msg -> {
					try {
						JSONObject resposta = Helper.get("https://api." + System.getenv("SERVER_URL") + "/reaction", new JSONObject() {{
							put("type", type);
						}}, null);

						Helper.logger(this.getClass()).debug(resposta);

						String url = resposta.getString("url");

						EmbedBuilder eb = new ColorlessEmbedBuilder();
						eb.setImage(url);
						if (allowReact) {
							eb.setFooter("↪ | Clique para retribuir");
							channel.sendMessage(message)
									.setEmbeds(eb.build())
									.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji("↪"), wrapper -> {
												if (wrapper.getUser().getId().equals(target.getId())) {
													answer(channel);
													s.editMessageComponents(List.of()).queue();
												}
											}), ShiroInfo.USE_BUTTONS, false, 1, TimeUnit.MINUTES, u -> u.getId().equals(target.getId()))
									);
						} else {
							channel.sendMessage(message).setEmbeds(eb.build()).queue();
						}
					} finally {
						msg.delete().queue(null, Helper::doNothing);
					}
				});
	}
}
