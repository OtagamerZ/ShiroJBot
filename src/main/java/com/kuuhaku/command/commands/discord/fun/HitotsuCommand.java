/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.fun;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.games.tabletop.games.hitotsu.Hitotsu;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(
		name = "hitotsu",
		aliases = {"uno"},
		usage = "req_bet-mentions",
		category = Category.FUN
)
@Requires({
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_ATTACH_FILES
})
public class HitotsuCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().isEmpty()) {
			channel.sendMessage(I18n.getString("err_no-user")).queue();
			return;
		}

		Kawaipon kp = Kawaipon.find(Kawaipon.class, author.getId());
		if (kp.getCards().size() < 25) {
			channel.sendMessage("❌ | É necessário ter ao menos 25 cartas para poder jogar Hitotsu.").queue();
			return;
		}

		for (User u : message.getMentionedUsers()) {
			Kawaipon k = Kawaipon.find(Kawaipon.class, u.getId());
			if (k.getCards().size() < 25) {
				channel.sendMessage(I18n.getString("err_not-enough-cards-mention", u.getAsMention())).queue();
				return;
			} else if (Main.getInfo().getConfirmationPending().get(u.getId()) != null) {
				channel.sendMessage("❌ | " + u.getAsMention() + " possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
				return;
			} else if (Main.getInfo().gameInProgress(u.getId())) {
				channel.sendMessage(I18n.getString("err_mention-in-game", u.getAsMention())).queue();
				return;
			} else if (u.getId().equals(author.getId())) {
				channel.sendMessage(I18n.getString("err_cannot-play-with-yourself")).queue();
				return;
			}
		}

		Account acc = Account.find(Account.class, author.getId());
		int bet = 0;

		if (args.length > 1 && StringUtils.isNumeric(args[0])) {
			bet = Integer.parseInt(args[0]);
			if (bet < 0) {
				channel.sendMessage(I18n.getString("err_invalid-credit-amount")).queue();
				return;
			} else if (acc.getBalance() < bet) {
				channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
				return;
			}

			for (User u : message.getMentionedUsers()) {
				Account a = Account.find(Account.class, u.getId());
				if (a.getBalance() < bet) {
					channel.sendMessage(I18n.getString("err_insufficient-credits-mention", u.getAsMention())).queue();
					return;
				}
			}
		}

		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
			return;
		}

		List<User> players = new ArrayList<>() {{
			add(author);
			addAll(message.getMentionedUsers());
		}};

		Set<String> accepted = new HashSet<>() {{
			add(author.getId());
		}};

		String msg;
		if (players.size() > 2)
			msg = CollectionHelper.parseAndJoin(message.getMentionedUsers(), IMentionable::getAsMention) + ", vocês foram desafiados a uma partida de Hitotsu, desejam aceitar?" + (bet != 0 ? " (aposta: " + StringHelper.separate(bet) + " CR)" : "");
		else
			msg = message.getMentionedUsers().get(0).getAsMention() + " você foi desafiado a uma partida de Hitotsu, deseja aceitar?" + (bet != 0 ? " (aposta: " + StringHelper.separate(bet) + " CR)" : "");

		for (User player : players) {
			Main.getInfo().getConfirmationPending().put(player.getId(), true);
		}
		int finalBet = bet;
		channel.sendMessage(msg).queue(s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
					if (players.contains(wrapper.getUser())) {
						if (Main.getInfo().gameInProgress(wrapper.getUser().getId())) {
							channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
							return;
						} else if (Main.getInfo().gameInProgress(author.getId())) {
							channel.sendMessage(I18n.getString("err_user-in-game")).queue();
							return;
						}

						if (!accepted.contains(wrapper.getUser().getId())) {
							channel.sendMessage(wrapper.getUser().getAsMention() + " aceitou a partida.").queue();
							accepted.add(wrapper.getUser().getId());
						}

						if (accepted.size() == players.size()) {
							Main.getInfo().getConfirmationPending().remove(author.getId());

							s.delete().queue(null, MiscHelper::doNothing);
							Game t = new Hitotsu(Main.getShiro(), channel, finalBet, players.toArray(User[]::new));
							t.start();
						}
					}
				}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
				u -> players.parallelStream().map(User::getId).anyMatch(i -> i.equals(u.getId())),
				ms -> {
					for (User player : players) {
						Main.getInfo().getConfirmationPending().remove(player.getId());
					}
				}
		));
	}
}
