package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Tradutor;
import com.kuuhaku.model.Anime;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import org.json.JSONObject;

import java.io.IOException;

public class AnimeCommand extends Command {

	public AnimeCommand() {
		super("anime", new String[]{"desenho", "cartoon"}, "Mostra dados sobre um anime.", Category.INFO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Você precisa digitar um nome para pesquisar.").queue();
			return;
		}
		channel.sendMessage("<a:Loading:598500653215645697> Buscando anime...").queue(m -> {
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

				String link = Helper.compareWithValues(com.kuuhaku.controller.Anime.getLink(anime.gettRomaji()), 403, 404) ? "Link indisponível" : "https://www.dreamanimes.com.br/anime-info/" + anime.gettRomaji().replace(" ", "-");

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
				eb.addField("Nota:", Float.toString(anime.getScore() / 10), true);
				eb.addField("Popularidade:", Integer.toString(anime.getPopularity()), true);
				eb.addField("Assista em:", link, true);

				if (!link.equalsIgnoreCase("Link indisponível")) {
					JSONObject jo = com.kuuhaku.controller.Anime.getDAData(anime.gettRomaji().replace(" ", "-"));
					eb.setDescription(jo.getString("Sinopse"));
					eb.addField("Gêneros:", jo.getJSONArray("tags").toString().replace("[", "`").replace("]", "`").replace(",", "` `").replace("\"", ""), false);
				} else {
					eb.setDescription(Tradutor.translate("en", "pt", anime.getDescription()));
					eb.addField("Gêneros:", anime.getGenres(), false);
				}

				eb.setFooter("Descrição traduzida por Yandex | http://translate.yandex.com.", "https://cdn6.aptoide.com/imgs/6/3/5/635bc7fad9a6329e0efbe9502f472dc5_icon.png");

				m.delete().queue();
				channel.sendMessage(eb.build()).queue();
			} catch (IOException e) {
				m.editMessage(":x: | Humm...não achei nenhum anime com esse nome, talvez você tenha escrito algo errado?").queue();
				Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
