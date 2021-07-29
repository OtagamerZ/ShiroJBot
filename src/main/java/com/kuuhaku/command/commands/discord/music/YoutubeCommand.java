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

package com.kuuhaku.command.commands.discord.music;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.Emote;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.Youtube;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.records.YoutubeVideo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.Music;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "play",
		aliases = {"youtube"},
		usage = "req_name",
		category = Category.MUSIC
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class YoutubeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa digitar um nome para pesquisar.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

		channel.sendMessage("<a:loading:697879726630502401> Buscando videos...").queue(m -> {
			try {
				List<YoutubeVideo> videos = Youtube.getData(String.join(" ", args));
				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setAuthor("Para ouvir essa música, conecte-se à um canal de voz e clique no botão ✅");

				m.editMessage(":mag: Resultados da busca").queue(s -> {
					if (videos.size() > 0) {
						List<Page> pages = new ArrayList<>();

						for (YoutubeVideo v : videos) {
							eb.setTitle(v.title(), v.getUrl())
									.setDescription(v.desc())
									.setThumbnail(v.thumb());

							pages.add(new Page(eb.build()));
						}

						channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(msg -> {
							if (Objects.requireNonNull(member.getVoiceState()).inVoiceChannel()) {
								Main.getInfo().getConfirmationPending().put(author.getId(), true);

								AtomicInteger p = new AtomicInteger();
								Pages.buttonize(msg, new LinkedHashMap<>() {{
											put(Pages.getPaginator().getEmote(Emote.PREVIOUS), (mb, ms) -> {
												if (p.get() > 0) {
													p.getAndDecrement();
													ms.editMessageEmbeds((MessageEmbed) pages.get(p.get()).getContent()).queue();
												}
											});
											put(Helper.ACCEPT, (mb, ms) -> {
												try {
													YoutubeVideo yv = videos.get(p.get());
													if (yv.playlist() && !TagDAO.getTagById(author.getId()).isBeta()) {
														channel.sendMessage("❌ | Você precisa ser um usuário com acesso beta para poder adicionar playlists.").queue();
														return;
													}

													Main.getInfo().getConfirmationPending().remove(author.getId());
													Music.loadAndPlay(member, channel, yv.getUrl());
													msg.delete().queue(null, Helper::doNothing);
												} catch (ErrorResponseException ignore) {
												}
											});
											put(Pages.getPaginator().getEmote(Emote.NEXT), (mb, ms) -> {
												if (p.get() < pages.size() - 1) {
													p.getAndIncrement();
													ms.editMessageEmbeds((MessageEmbed) pages.get(p.get()).getContent()).queue();
												}
											});
										}}, true, 1, TimeUnit.MINUTES,
										u -> u.getId().equals(author.getId()),
										ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
								);
							} else {
								Pages.paginate(msg, pages, 1, TimeUnit.MINUTES);
							}
						});
					} else m.editMessage("❌ | Nenhum vídeo encontrado").queue();
				});
			} catch (IOException e) {
				m.editMessage("❌ | Erro ao buscar vídeos, meus desenvolvedores já foram notificados.").queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
