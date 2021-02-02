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

package com.kuuhaku.command.commands.discord.beta;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.handlers.games.kawaigotchi.enums.Race;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;

@Command(
		name = "pkgotchi",
		aliases = {"buykgotchi", "comprarkgotchi"},
		usage = "req_kgotchi",
		category = Category.BETA
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class PurchaceKGotchiCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (KGotchiDAO.getKawaigotchi(author.getId()) != null || com.kuuhaku.controller.sqlite.KGotchiDAO.getKawaigotchi(author.getId()) != null) {
			channel.sendMessage("❌ | Você já possui um Kawaigotchi!").queue();
			return;
		}

		if (args.length == 0) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setTitle("Bem vindo(a) à loja de Kawaigotchis!");
			eb.setDescription("Kawaigotchis são animais fofinhos que lhe ajudarão a ganhar mais experiência para o seu perfil, assim como acompanhá-lo nos servidores que eu estou!");
			eb.addField("Aleatório (500 créditos)\n`" + prefix + "pkgotchi aleatorio NOME`", "Você ganha um Kawaigotchi de raça, natureza e cor aleatórias.", true);
			//eb.addField("Escolher (2500 créditos)\n`" + prefix + "pkgotchi escolher RAÇA NOME`", "Você escolhe a raça de seu Kawaigotchi, ele ainda terá natureza e cor aleatórias.", true);
			eb.setThumbnail("https://lens-storage.storage.googleapis.com/png/7314bb86-3d18-425c-8e95-0bebf4135060");
			eb.setFooter("Seus créditos: " + Helper.separate(acc.getBalance()), "https://i.imgur.com/U0nPjLx.gif");

			channel.sendMessage(eb.build()).queue();
			return;
		}

		switch (args[0]) {
			/*case "escolher" -> {
				if (acc.getTotalBalance() < 2500) {
					channel.sendMessage("❌ | Você não possui créditos suficientes (seus créditos: " + Helper.tSeparator(acc.getBalance()) + ")!").queue();
					return;
				} else if (args.length == 2) {
					String[] image = {
							"https://i.imgur.com/uUEpxbI.png"
					};
					String[] title = {
							"Snugget"
					};
					String[] description = {
							"Os snuggets são adoráveis aglomerados de plasma estável. Eles são dóceis, curiosos e felpudos, certamente a escolha perfeita para um verdadeiro viajante das estrelas!"
					};

					List<Page> pages = new ArrayList<>();
					EmbedBuilder eb = new EmbedBuilder();

					for (int i = 0; i < image.length; i++) {
						eb.clear();

						eb.setTitle(title[i]);
						eb.setDescription(description[i]);
						eb.setThumbnail(image[i]);

						pages.add(new Page(PageType.EMBED, eb.build()));
					}

					channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(m, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
					return;
				} else if (args.length < 3) {
					channel.sendMessage("❌ | Você precisa dar um nome ao seu Kawaigotchi!").queue();
					return;
				}

				String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

				if (name.length() > 15) {
					channel.sendMessage("❌ | Nome muito longo, escolha um nome menor!").queue();
					return;
				}

				switch (args[1].toLowerCase()) {
					case "snugget":
						acc.consumeCredit(2500, this.getClass());
						Kawaigotchi k = new Kawaigotchi(author.getId(), name, Race.SNUGGET);

						KGotchiDAO.saveKawaigotchi(k);
						com.kuuhaku.controller.sqlite.KGotchiDAO.saveKawaigotchi(k);
						AccountDAO.saveAccount(acc);

						channel.sendMessage("Seu mais novo Kawaigotchi é um...**Snugget**!").queue();
						break;
					default:
						channel.sendMessage("❌ | Você precisa escolher uma raça válida para seu Kawaigotchi!").queue();
				}
			}*/
			case "aleatorio" -> {
				if (acc.getTotalBalance() < 500) {
					channel.sendMessage("❌ | Você não possui créditos suficientes (seus créditos: " + Helper.separate(acc.getBalance()) + ")!").queue();
					return;
				} else if (args.length < 2) {
					channel.sendMessage("❌ | Você precisa dar um nome ao seu Kawaigotchi!").queue();
					return;
				}
				String nome = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				if (nome.length() > 15) {
					channel.sendMessage("❌ | Nome muito longo, escolha um nome menor!").queue();
					return;
				}
				acc.consumeCredit(500, this.getClass());
				Kawaigotchi k = new Kawaigotchi(author.getId(), nome, Race.SNUGGET/*Race.values()[Helper.rng(2)]*/);
				KGotchiDAO.saveKawaigotchi(k);
				com.kuuhaku.controller.sqlite.KGotchiDAO.saveKawaigotchi(k);
				AccountDAO.saveAccount(acc);
				channel.sendMessage("Seu mais novo Kawaigotchi é um...**" + k.getRace() + "**!").queue();
			}
		}
	}
}
