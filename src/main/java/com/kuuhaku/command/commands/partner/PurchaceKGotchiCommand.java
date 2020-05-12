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

package com.kuuhaku.command.commands.partner;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.handlers.games.kawaigotchi.enums.Race;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PurchaceKGotchiCommand extends Command {

	public PurchaceKGotchiCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PurchaceKGotchiCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PurchaceKGotchiCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PurchaceKGotchiCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (KGotchiDAO.getKawaigotchi(author.getId()) != null || com.kuuhaku.controller.sqlite.KGotchiDAO.getKawaigotchi(author.getId()) != null) {
			channel.sendMessage(":x: | Você já possui um Kawaigotchi!").queue();
			return;
		}

		if (args.length == 0) {
			EmbedBuilder eb = new EmbedBuilder();

			eb.setTitle("Bem vindo(a) à loja de Kawaigotchis!");
			eb.setDescription("Kawaigotchis são animais fofinhos que lhe ajudarão a ganhar mais experiência para o seu perfil, assim como acompanhá-lo nos servidores que eu estou.\n\nValhe a pena notar que ele será \"pausado\" enquanto você estiver offline, então não precisa se preocupar!");
			eb.addField("Aleatório (500 créditos)\n`" + prefix + "pkgotchi aleatorio NOME`", "Você ganha um Kawaigotchi de raça, natureza e cor aleatórias.", true);
			eb.addField("Escolher (2500 créditos)\n`" + prefix + "pkgotchi escolher RAÇA NOME`", "Você escolhe a raça de seu Kawaigotchi, ele ainda terá natureza e cor aleatórias.", true);
			eb.setThumbnail("https://lens-storage.storage.googleapis.com/png/7314bb86-3d18-425c-8e95-0bebf4135060");
			eb.setFooter("Seus créditos: " + acc.getBalance(), "https://i.imgur.com/U0nPjLx.gif");
			eb.setColor(Helper.getRandomColor());

			channel.sendMessage(eb.build()).queue();
			return;
		}

		switch (args[0]) {
			case "escolher":
				if (acc.getBalance() < 2500) {
					channel.sendMessage(":x: | Você não possui créditos suficientes (seus créditos: " + acc.getBalance() + ")!").queue();
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
						try {
							eb.setColor(Helper.colorThief(image[i]));
						} catch (IOException e) {
							eb.setColor(Helper.getRandomColor());
						}

						pages.add(new Page(PageType.EMBED, eb.build()));
					}

					channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(m, pages, 60, TimeUnit.SECONDS, 5));
					return;
				} else if (args.length < 3) {
					channel.sendMessage(":x: | Você precisa dar um nome ao seu Kawaigotchi!").queue();
					return;
				}

				String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

				if (name.length() > 15) {
					channel.sendMessage(":x: | Nome muito longo, escolha um nome menor!").queue();
					return;
				}

				switch (args[1].toLowerCase()) {
					case "snugget":
						acc.removeCredit(2500);
						Kawaigotchi k = new Kawaigotchi(author.getId(), name, Race.SNUGGET);

						KGotchiDAO.saveKawaigotchi(k);
						com.kuuhaku.controller.sqlite.KGotchiDAO.saveKawaigotchi(k);
						AccountDAO.saveAccount(acc);

						channel.sendMessage("Seu mais novo Kawaigotchi é um...**Snugget**!").queue();
						break;
					default:
						channel.sendMessage(":x: | Você precisa escolher uma raça válida para seu Kawaigotchi!").queue();
				}
				break;
			case "aleatorio":
				if (acc.getBalance() < 500) {
					channel.sendMessage(":x: | Você não possui créditos suficientes (seus créditos: " + acc.getBalance() + ")!").queue();
					return;
				} else if (args.length < 2) {
					channel.sendMessage(":x: | Você precisa dar um nome ao seu Kawaigotchi!").queue();
					return;
				}

				String nome = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

				if (nome.length() > 15) {
					channel.sendMessage(":x: | Nome muito longo, escolha um nome menor!").queue();
					return;
				}

				acc.removeCredit(500);
				Kawaigotchi k = new Kawaigotchi(author.getId(), nome, Race.SNUGGET/*Race.values()[Helper.rng(2)]*/);

				KGotchiDAO.saveKawaigotchi(k);
				com.kuuhaku.controller.sqlite.KGotchiDAO.saveKawaigotchi(k);
				AccountDAO.saveAccount(acc);

				channel.sendMessage("Seu mais novo Kawaigotchi é um...**" + k.getRace() + "**!").queue();
				break;
		}
	}
}
