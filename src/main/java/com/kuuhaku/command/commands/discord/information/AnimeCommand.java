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
import com.kuuhaku.model.common.Anime;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

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
				JSONObject data = AnimeRequest.getData(String.join(" ", args), query);
				try {
					Anime anime = new Anime(data);

					EmbedBuilder eb = new EmbedBuilder();
					boolean hentai = anime.getGenres().toLowerCase(Locale.ROOT).contains("hentai");
					if (hentai && !message.getTextChannel().isNSFW()) {
						m.editMessage("Humm safadinho, eu não posso postar sobre Hentais neste canal!").queue();
						return;
					}

					JSONObject jo = hentai ? AnimeRequest.getMHData(anime.gettRomaji()) : AnimeRequest.getNAData(anime.gettRomaji());

					String link;
					if (jo.has("desc")) {
						if (hentai) {
							link = "[Mega Hentais](https://www.megahentais.com/?page_id=%s&ref=%s)".formatted(
									jo.getInt("id"),
									Helper.hash(System.getenv("MEGAHENTAIS_TOKEN").getBytes(StandardCharsets.UTF_8), "SHA-1")
							);
						} else {
							link = "[Now Animes](https://www.nowanimes.com/?page_id=%s&ref=%s)".formatted(
									jo.getInt("id"),
									Helper.hash(System.getenv("NOWANIMES_TOKEN").getBytes(StandardCharsets.UTF_8), "SHA-1")
							);
						}
					} else {
						link = "Link indisponível";
					}

					eb.setColor(anime.getcColor());
					if (hentai) eb.setAuthor("Bem, aqui está um novo hentai para você assistir! ( ͡° ͜ʖ ͡°)\n");
					else eb.setAuthor("Bem, aqui está um novo anime para você assistir!\n");

					eb.setTitle(anime.gettRomaji() + (!anime.gettRomaji().equals(anime.gettEnglish()) ? " (" + anime.gettEnglish() + ")" : ""))
							.setImage(anime.getcImage())
							.addField("Estúdio:", anime.getStudio(), true)
							.addField("Criado por:", anime.getCreator(), true)
							.addField("Ano:", anime.getsDate(), true)
							.addField("Estado:", anime.getStatus(), true)
							.addField("Episódios:", anime.getDuration(), true);

					if (anime.getNaeAiringAt() != null)
						eb.addField("Próximo episódio:", anime.getNaeAiringAt(), true);

					eb.addField("Nota:", anime.getScore() == -1 ? "Nenhuma" : Float.toString(anime.getScore() / 10), true)
							.addField("Popularidade:", Integer.toString(anime.getPopularity()), true)
							.addField("Assista em:", link, true);

					if (!link.equalsIgnoreCase("Link indisponível")) {
						eb.setDescription(StringUtils.abbreviate(jo.getString("desc"), 2048));
					} else {
						eb.setDescription(
								StringUtils.abbreviate(
										anime.getDescription()
												.replace("<br>", "\n")
												.replaceAll("(<i>|</i>)", "_")
												.replaceAll("(<b>|</b>)", "**")
												.replaceAll("(<u>|</u>)", "__")
												.replaceAll("(<p>|</p>)", "")
										, 2048)
						);
					}
					eb.addField("Gêneros:", anime.getGenres(), false);

					m.delete().queue();
					channel.sendMessage(eb.build()).queue();
				} catch (JSONException e) {
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
