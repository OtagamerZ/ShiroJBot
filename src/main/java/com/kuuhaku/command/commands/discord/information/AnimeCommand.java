/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.records.anime.Anime;
import com.kuuhaku.model.records.anime.Media;
import com.kuuhaku.model.records.anime.NAMHData;
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
					if (anime == null) {
						m.editMessage(I18n.getString("err_anime-not-found")).queue();
						return;
					}

					Media media = anime.data().media();
					if (media == null) throw new IllegalStateException();

					EmbedBuilder eb = new EmbedBuilder();
					boolean hentai = media.genres().stream().anyMatch("hentai"::equalsIgnoreCase);
					if (hentai && !message.getTextChannel().isNSFW()) {
						m.editMessage("Humm safadinho, eu não posso postar sobre Hentais neste canal!").queue();
						return;
					}

					NAMHData namh = null;
					JSONObject res;

					if (hentai) res = AnimeRequest.getMHData(media.title().romaji());
					else res = AnimeRequest.getNAData(media.title().romaji());

					if (res != null)
						namh = JSONUtils.fromJSON(res.toString(), NAMHData.class);

					String link;
					if (namh != null) {
						if (hentai) {
							link = "[Mega Hentais](https://www.megahentais.com/?page_id=%s&ref=%s)".formatted(
									namh.id(),
									Helper.hash(System.getenv("MEGAHENTAIS_TOKEN").getBytes(StandardCharsets.UTF_8), "SHA-1")
							);
						} else {
							link = "[Now Animes](https://www.nowanimes.com/?page_id=%s&ref=%s)".formatted(
									namh.id(),
									Helper.hash(System.getenv("NOWANIMES_TOKEN").getBytes(StandardCharsets.UTF_8), "SHA-1")
							);
						}
					} else {
						link = "Link indisponível";
					}

					eb.setColor(media.coverImage().getParsedColor());
					if (hentai) eb.setAuthor("Bem, aqui está um novo hentai para você assistir! ( ͡° ͜ʖ ͡°)");
					else eb.setAuthor("Bem, aqui está um novo anime para você assistir!");

					eb.setTitle(media.title().romaji() + (media.title().romaji().equals(media.title().english()) ? "" : " (" + media.title().english() + ")"))
							.setImage(media.coverImage().extraLarge())
							.addField("Estúdio:", media.studios().getMajor(), true)
							.addField("Criado por:", media.staff().getCreator(), true)
							.addField("Ano:", String.valueOf(media.startDate().year()), true)
							.addField("Estado:", media.status().equals("FINISHED") ? "Finalizado" : "Em lançamento", true)
							.addField("Episódios:", String.valueOf(media.episodes()), true);

					if (media.nextAiringEpisode() != null) {
						eb.addField("Próximo episódio (ep. " + media.nextAiringEpisode().episode() + "):", Helper.fullDateFormat.format(media.nextAiringEpisode().getAiringAtDate()), true);
					}

					eb.addField("Nota:", media.averageScore() == 0 ? "Nenhuma" : String.valueOf(media.averageScore()), true)
							.addField("Popularidade:", String.valueOf(media.popularity()), true)
							.addField("Assista em:", link, true);

					if (!link.equalsIgnoreCase("Link indisponível")) {
						eb.setDescription(StringUtils.abbreviate(namh.desc(), 2048));
					} else {
						eb.setDescription(
								StringUtils.abbreviate(Helper.htmlConverter.convert(media.description()), 2048)
						);
					}
					eb.addField("Gêneros:", media.genres().stream().map(s -> "`" + s + "`").collect(Collectors.joining(", ")), false);

					m.delete().queue();
					channel.sendMessageEmbeds(eb.build()).queue();
				} catch (IllegalStateException | IllegalArgumentException e) {
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
