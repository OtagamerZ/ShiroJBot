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

package com.kuuhaku.command.commands.music;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Youtube;
import com.kuuhaku.model.common.YoutubeVideo;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;

public class VideoCommand extends Command {

	public VideoCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public VideoCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public VideoCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public VideoCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Você precisa digitar um nome para pesquisar.").queue();
			return;
		}
		channel.sendMessage("<a:Loading:598500653215645697> Buscando video...").queue(m -> {
			try {
				YoutubeVideo v = Youtube.getSingleData(String.join(" ", args));
				EmbedBuilder eb = new EmbedBuilder();

				m.editMessage(":mag: Resultados da busca").queue(s -> {
					try {
						eb.setTitle(v.getTitle(), v.getUrl());
						eb.setDescription(v.getDesc());
						eb.setImage(v.getThumb());
						eb.setColor(Helper.colorThief(v.getThumb()));
						eb.setFooter("Link: " + v.getUrl(), null);
						channel.sendMessage(eb.build()).queue(msg -> {
							Helper.playAudio(member, message, (TextChannel) channel, guild, msg);
						});
					} catch (IOException e) {
						m.editMessage(":x: | Nenhum vídeo encontrado.").queue();
						Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
					}
				});
			} catch (IOException e) {
				m.editMessage(":x: | Erro ao buscar vídeos, meus developers já foram notificados.").queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
