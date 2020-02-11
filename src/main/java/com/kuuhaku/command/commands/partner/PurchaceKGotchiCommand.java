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

package com.kuuhaku.command.commands.partner;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.AccountDAO;
import com.kuuhaku.controller.mysql.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.handlers.games.kawaigotchi.enums.Race;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;

public class PurchaceKGotchiCommand extends Command {

	public PurchaceKGotchiCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public PurchaceKGotchiCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public PurchaceKGotchiCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public PurchaceKGotchiCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
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
			eb.addField("Como compro um?", "Para comprar um Kawaigotchi de raça aleatória você precisará de 500 créditos que podem ser adquiridos votando em mim, agora, caso você deseje escolher a raça, serão necessários 2500 créditos!", true);
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
						channel.sendMessage(":x: | Você precisa escolher uma raça para seu Kawaigotchi!").queue();
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
