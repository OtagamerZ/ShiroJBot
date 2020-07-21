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

package com.kuuhaku.command.commands.fun;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.hitotsu.Hitotsu;
import com.kuuhaku.handlers.games.tabletop.entity.Tabletop;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HitotsuCommand extends Command {

	public HitotsuCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public HitotsuCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public HitotsuCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public HitotsuCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
			return;
		}

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		if (kp.getCards().size() < 25) {
			channel.sendMessage(":x: | É necessário ter ao menos 25 cartas para poder jogar Hitotsu.").queue();
			return;
		}

		for (User u : message.getMentionedUsers()) {
			if (KawaiponDAO.getKawaipon(message.getMentionedUsers().get(0).getId()).getCards().size() < 25) {
				channel.sendMessage(":x: | " + u.getAsMention() + " não possui cartas suficientes, é necessário ter ao menos 25 cartas para poder jogar Hitotsu.").queue();
				return;
			}
		}

		Account uacc = AccountDAO.getAccount(author.getId());
		int bet = 0;
		if (args.length > 1 && StringUtils.isNumeric(args[1])) {
			bet = Integer.parseInt(args[1]);
			if (bet < 0) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-credit-amount")).queue();
				return;
			} else if (uacc.getBalance() < bet) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
				return;
			}

			for (User u : message.getMentionedUsers()) {
				Account tacc = AccountDAO.getAccount(u.getId());
				if (tacc.getBalance() < bet) {
					channel.sendMessage(String.format(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-mention"), u.getAsMention())).queue();
					return;
				}
			}
		}

		String id = author.getId() + "." + message.getMentionedUsers().stream().map(User::getAsMention).map(s -> s + ".").collect(Collectors.joining()) + "." + guild.getId();

		if (ShiroInfo.gameInProgress(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
			return;
		}

		for (User u : message.getMentionedUsers()) {
			if (ShiroInfo.gameInProgress(u.getId())) {
				channel.sendMessage(String.format(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game"), u.getAsMention())).queue();
				return;
			} else if (u.getId().equals(author.getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-play-with-yourself")).queue();
				return;
			}
		}

		List<User> players = new ArrayList<>(message.getMentionedUsers());
		players.add(author);
		Tabletop t = new Hitotsu((TextChannel) channel, id, players.toArray(User[]::new));
		int finalBet = bet;
		if (message.getMentionedUsers().size() == 1)
			channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " você foi desafiado a uma partida de Hitotsu, deseja aceitar?" + (bet != 0 ? " (aposta: " + bet + " créditos)" : ""))
					.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
						if (mb.getId().equals(message.getMentionedUsers().get(0).getId())) {
							if (ShiroInfo.gameInProgress(message.getMentionedUsers().get(0).getId())) {
								channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game")).queue();
								return;
							}
							ShiroInfo.getGames().put(id, t);
							ms.delete().queue();
							t.execute(finalBet);
						}
					}), false, 1, TimeUnit.MINUTES));
		else
			channel.sendMessage(players.stream().map(User::getAsMention).map(s -> s + ", ").collect(Collectors.joining()) + " vocês foram desafiados a uma partida de Hitotsu, desejam aceitar?" + (bet != 0 ? " (aposta: " + bet + " créditos)" : ""))
					.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
						int accepted = 1;
						if (players.contains(mb.getUser())) accepted++;
						if (accepted == players.size()) {
							if (ShiroInfo.gameInProgress(author.getId())) {
								channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game")).queue();
								return;
							}
							ShiroInfo.getGames().put(id, t);
							ms.delete().queue();
							t.execute(finalBet);
						}
					}), false, 1, TimeUnit.MINUTES));
	}
}
