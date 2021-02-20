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

import com.kuuhaku.command.Executable;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*@Command(
		name = "imagem",
		aliases = {"image", "img"},
		usage = "req_name",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES})*/
public class ImageCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa indicar o que devo pesquisar.").queue();
			return;
		}

		String query = String.join(" ", args);

		channel.sendMessage("<a:loading:697879726630502401> Buscando imagem...").queue(m -> {
			try {
				URL link = new URL("https://www.googleapis.com/customsearch/v1?key=" + System.getenv("GOOGLE_TOKEN") + "&cx=" + System.getenv("GOOGLE_SEARCH") + "&searchType=image&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
				HttpsURLConnection con = (HttpsURLConnection) link.openConnection();
				con.setRequestMethod("GET");
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				Helper.logger(this.getClass()).debug("Requisição 'GET' para o URL: " + link);
				Helper.logger(this.getClass()).debug("Resposta: " + con.getResponseCode());
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

				String input;
				StringBuilder resposta = new StringBuilder();
				while ((input = br.readLine()) != null) {
					resposta.append(input);
				}
				br.close();

				Helper.logger(this.getClass()).debug(resposta.toString());
				JSONObject jo = new JSONObject(resposta.toString());
				JSONArray items = jo.getJSONArray("items");
				JSONObject item = items.getJSONObject(Helper.rng(items.length(), true));
				JSONObject image = item.getJSONObject("image");

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Helper.colorThief(item.getString("link")));
				eb.setAuthor("Aqui está!");
				eb.setTitle(item.getString("title"), image.getString("contextLink"));
				eb.addField("Largura:", Integer.toString(image.getInt("width")), true);
				eb.addField("Altura:", Integer.toString(image.getInt("height")), true);
				eb.addField("Tamanho: ", Helper.round(image.getInt("byteSize") / 1024f / 1024f, 2) + " MB", true);
				eb.setImage(item.getString("link"));

				m.delete().queue();
				channel.sendMessage(eb.build()).queue();
			} catch (IOException | JSONException e) {
				m.editMessage("❌ | Humm...não achei nenhuma imagem com esses termos, talvez você tenha escrito algo errado?").queue();
			}
		});
	}
}
