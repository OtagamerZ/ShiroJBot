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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Tradutor;
import com.kuuhaku.model.common.Anime;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class AnimeCommand extends Command {

	public AnimeCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public AnimeCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public AnimeCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public AnimeCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-name")).queue();
			return;
		}

		channel.sendMessage("<a:loading:697879726630502401> Buscando anime...").queue(m -> {
			try {
				String query = "{\n" +
						"Media(search: \\\"" + String.join(" ", args) + "\\\", type: ANIME) {\n" +
						"idMal\n" +
						"title {\n" +
						"romaji\n" +
						"english\n" +
						"}\n" +
						"status\n" +
						"startDate {\n" +
						"year\n" +
						"month\n" +
						"day\n" +
						"}\n" +
						"episodes\n" +
						"coverImage {\n" +
						"extraLarge\n" +
						"large\n" +
						"medium\n" +
						"color\n" +
						"}\n" +
						"genres\n" +
						"averageScore\n" +
						"popularity\n" +
						"studios(isMain: true) {\n" +
						"edges {\n" +
						"node {\n" +
						"name\n" +
						"}\n" +
						"}\n" +
						"}\n" +
						"staff {\n" +
						"edges {\n" +
						"role\n" +
						"node {\n" +
						"name {\n" +
						"first\n" +
						"last\n" +
						"}\n" +
						"}\n" +
						"}\n" +
						"}" +
						"nextAiringEpisode {\n" +
						"episode\n" +
						"airingAt\n" +
						"}\n" +
						"trailer {\n" +
						"site\n" +
						"}\n" +
						"description\n" +
						"}\n" +
						"}\n";
				query = query.replace("\n", " ");
				JSONObject data = new JSONObject(com.kuuhaku.controller.Anime.getData(query));
				Anime anime = new Anime(data);

				EmbedBuilder eb = new EmbedBuilder();
				if (anime.getGenres().toLowerCase().contains("hentai") && !message.getTextChannel().isNSFW()) {
					m.editMessage("Humm safadinho, eu não posso postar sobre Hentais neste canal!").queue();
					return;
				}

				JSONObject jo = com.kuuhaku.controller.Anime.getDAData(anime.gettRomaji().replace(" ", "-"));

				String link = (jo.has("url") ? jo.getString("url") : "Link indisponível").replace("api/", "");

				eb.setColor(anime.getcColor());
				eb.setAuthor("Bem, aqui está um novo anime para você assistir!\n");
				eb.setTitle(anime.gettRomaji() + (!anime.gettRomaji().equals(anime.gettEnglish()) ? " (" + anime.gettEnglish() + ")" : ""));

				eb.setImage(anime.getcImage());
				eb.addField("Estúdio:", anime.getStudio(), true);
				eb.addField("Criado por:", anime.getCreator(), true);
				eb.addField("Ano:", anime.getsDate(), true);
				eb.addField("Estado:", anime.getStatus(), true);
				eb.addField("Episódios:", anime.getDuration(), true);
				if (anime.getNaeAiringAt() != null)
					eb.addField("Próximo episódio:", anime.getNaeAiringAt(), true);
				eb.addField("Nota:", anime.getScore() == -1 ? "Nenhuma" : Float.toString(anime.getScore() / 10), true);
				eb.addField("Popularidade:", Integer.toString(anime.getPopularity()), true);
				eb.addField("Assista em:", link, true);

				if (!link.equalsIgnoreCase("Link indisponível")) {
					eb.setDescription(jo.getJSONObject("anime").getString("Sinopse"));
					eb.addField("Gêneros:", jo.getJSONObject("anime").getJSONArray("tags").toString().replace("[", "`").replace("]", "`").replace(",", "` `").replace("\"", ""), false);
				} else {
					eb.setDescription(Tradutor.translate("en", "pt", anime.getDescription()));
					eb.addField("Gêneros:", anime.getGenres(), false);
				}

				m.delete().queue();
				channel.sendMessage(eb.build()).queue();
			} catch (IOException | JSONException e) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_anime-not-found")).queue();
				Helper.logger(this.getClass()).debug(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
