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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConvertCardCommand implements Executable {

	public ConvertCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ConvertCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ConvertCardCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ConvertCardCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa digitar o nome da carta kawaipon que quer converter para carta senshi.").queue();
			return;
		}

		Card tc = CardDAO.getCard(args[0], true);
		if (tc == null) {
			channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + Helper.didYouMean(args[0], CardDAO.getAllCardNames().toArray(String[]::new)) + "`?").queue();
			return;
		} else if (tc.getId().equals(tc.getAnime().name())) {
			channel.sendMessage("❌ | Você não pode converter cartas Ultimate.").queue();
			return;
		}

		KawaiponCard kc = new KawaiponCard(tc, false);

		if (!kp.getCards().contains(kc)) {
			channel.sendMessage("❌ | Você não possui essa carta.").queue();
			return;
		}

		Champion c = CardDAO.getChampion(tc);

		if (c == null || c.isFusion()) {
			channel.sendMessage("❌ | Essa carta não é elegível para conversão.").queue();
			return;
		} else if (kp.getChampions().stream().filter(c::equals).count() == 3) {
			channel.sendMessage("❌ | Você só pode ter no máximo 3 cópias de cada carta no seu deck.").queue();
			return;
		} else if (kp.getChampions().size() == 36) {
			channel.sendMessage("❌ | Você só pode ter no máximo 36 cartas senshi no seu deck.").queue();
			return;
		}

		c.setAcc(acc);
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		eb.setTitle("Por favor confirme!");
		eb.setDescription("Sua carta kawaipon " + kc.getName() + " será convertida para carta senshi e será adicionada ao seu deck, por favor clique no botão abaixo para confirmar a conversão.");
		eb.setImage("attachment://card.png");

		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage(eb.build()).addFile(Helper.getBytes(c.drawCard(false), "png"), "card.png").queue(s ->
				Pages.buttonize(s, Map.of(Helper.ACCEPT, (ms, mb) -> {
							if (!ShiroInfo.getHashes().remove(hash)) return;
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
							kp.removeCard(kc);
							kp.addChampion(c);
							KawaiponDAO.saveKawaipon(kp);
							s.delete().queue();
							channel.sendMessage("✅ | Conversão realizada com sucesso!").queue();
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						ms -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
		);
	}
}
