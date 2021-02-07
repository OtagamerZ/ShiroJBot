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

package com.kuuhaku.command.commands.discord.fun;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.games.tabletop.games.chess.Chess;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/*@Command(
		name = "xadrez",
		aliases = {"chess"},
		usage = "req_bet-mentions",
		category = Category.FUN
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})*/
public class ChessCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().getIfPresent(message.getMentionedUsers().get(0).getId()) != null) {
			channel.sendMessage("❌ | Este usuário possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
			return;
		}

		Account uacc = AccountDAO.getAccount(author.getId());
		Account tacc = AccountDAO.getAccount(message.getMentionedUsers().get(0).getId());
		int bet = 0;
		if (args.length > 1 && StringUtils.isNumeric(args[1])) {
			bet = Integer.parseInt(args[1]);
			if (bet < 0) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-credit-amount")).queue();
				return;
			} else if (uacc.getBalance() < bet) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
				return;
			} else if (tacc.getBalance() < bet) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-target")).queue();
				return;
			}
		}

		String id = author.getId() + "." + message.getMentionedUsers().get(0).getId() + "." + guild.getId();

		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
			return;
		} else if (Main.getInfo().gameInProgress(message.getMentionedUsers().get(0).getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game")).queue();
			return;
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-play-with-yourself")).queue();
			return;
		}

		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		Game t = new Chess(Main.getShiroShards(), channel, bet, author, message.getMentionedUsers().get(0));
		channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " você foi desafiado a uma partida de Xadrez, deseja aceitar?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							if (mb.getId().equals(message.getMentionedUsers().get(0).getId())) {
								if (!ShiroInfo.getHashes().remove(hash)) return;
								Main.getInfo().getConfirmationPending().invalidate(author.getId());
								if (Main.getInfo().gameInProgress(mb.getId())) {
									channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
									return;
								} else if (Main.getInfo().gameInProgress(message.getMentionedUsers().get(0).getId())) {
									channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game")).queue();
									return;
								}

								//Main.getInfo().getGames().put(id, t);
								s.delete().queue(null, Helper::doNothing);
								t.start();
							}
						}), true, 1, TimeUnit.MINUTES,
						u -> Helper.equalsAny(u.getId(), author.getId(), message.getMentionedUsers().get(0).getId()),
						ms -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
				);
	}
}
