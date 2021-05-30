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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.AnimeRequest;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.anime.Anime;
import com.kuuhaku.model.common.anime.Media;
import com.kuuhaku.model.common.anime.NAMHData;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.JSONUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

@Command(
		name = "anime",
		aliases = {"desenho", "cartoon"},
		usage = "req_name",
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EXT_EMOJI
})
public class AnimeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(I18n.getString("err_no-name")).queue();
			return;
		}

		channel.sendMessage("<a:loading:697879726630502401> Buscando anime...").queue(m -> {
			try {
				String query = IOUtils.toString(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("anilist.graphql")), StandardCharsets.UTF_8);
				JSONObject data = AnimeRequest.getData(argsAsText, query);
				try {
					Anime anime = JSONUtils.fromJSON(data.toString(), Anime.class);
					Media media = anime.getMedia();

					EmbedBuilder eb = new EmbedBuilder();
					boolean hentai = media.getGenres().stream().anyMatch("hentai"::equalsIgnoreCase);
					if (hentai && !message.getTextChannel().isNSFW()) {
						m.editMessage("Humm safadinho, eu não posso postar sobre Hentais neste canal!").queue();
						return;
					}

					NAMHData namh;
					if (hentai)
						namh = JSONUtils.fromJSON(AnimeRequest.getMHData(media.getTitle().getRomaji()).toString(), NAMHData.class);
					else
						namh = JSONUtils.fromJSON(AnimeRequest.getNAData(media.getTitle().getRomaji()).toString(), NAMHData.class);

					String link;
					if (namh != null) {
						if (hentai) {
							link = "[Mega Hentais](https://www.megahentais.com/?page_id=%s&ref=%s)".formatted(
									namh.getId(),
									Helper.hash(System.getenv("MEGAHENTAIS_TOKEN").getBytes(StandardCharsets.UTF_8), "SHA-1")
							);
						} else {
							link = "[Now Animes](https://www.nowanimes.com/?page_id=%s&ref=%s)".formatted(
									namh.getId(),
									Helper.hash(System.getenv("NOWANIMES_TOKEN").getBytes(StandardCharsets.UTF_8), "SHA-1")
							);
						}
					} else {
						link = "Link indisponível";
					}

					eb.setColor(media.getCoverImage().getParsedColor());
					if (hentai) eb.setAuthor("Bem, aqui está um novo hentai para você assistir! ( ͡° ͜ʖ ͡°)");
					else eb.setAuthor("Bem, aqui está um novo anime para você assistir!");

					eb.setTitle(media.getTitle().getRomaji() + (media.getTitle().getRomaji().equals(media.getTitle().getEnglish()) ? "" : " (" + media.getTitle().getEnglish() + ")"))
							.setImage(media.getCoverImage().getExtraLarge())
							.addField("Estúdio:", media.getStudios().getMajor(), true)
							.addField("Criado por:", media.getStaff().getCreator(), true)
							.addField("Ano:", Helper.dateFormat.format(media.getStartDate().getDate()), true)
							.addField("Estado:", media.getStatus().equals("FINISHED") ? "Finalizado" : "Em lançamento", true)
							.addField("Episódios:", String.valueOf(media.getEpisodes()), true);

					if (media.getNextAiringEpisode() != null) {
						eb.addField("Próximo episódio (ep. " + media.getNextAiringEpisode().getEpisode() + "):", Helper.fullDateFormat.format(media.getNextAiringEpisode().getAiringAtDate()), true);
					}

					eb.addField("Nota:", media.getAverageScore() == 0 ? "Nenhuma" : String.valueOf(media.getAverageScore()), true)
							.addField("Popularidade:", String.valueOf(media.getPopularity()), true)
							.addField("Assista em:", link, true);

					if (!link.equalsIgnoreCase("Link indisponível")) {
						eb.setDescription(StringUtils.abbreviate(namh.getDesc(), 2048));
					} else {
						eb.setDescription(
								StringUtils.abbreviate(Helper.htmlConverter.convert(media.getDescription()), 2048)
						);
					}
					eb.addField("Gêneros:", media.getGenres().stream().map(s -> "`" + s + "`").collect(Collectors.joining(", ")), false);

					m.delete().queue();
					channel.sendMessage(eb.build()).queue();
				} catch (IllegalStateException e) {
					m.editMessage(I18n.getString("err_anime-not-found")).queue();
					Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
					Helper.logger(this.getClass()).warn(data.toString());
				}
			} catch (IOException e) {
				Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
