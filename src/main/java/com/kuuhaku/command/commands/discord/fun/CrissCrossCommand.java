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
import com.kuuhaku.handlers.games.tabletop.games.crisscross.CrissCross;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.LogicHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
        name = "jogodavelha",
        aliases = {"jdv", "crisscross", "cc"},
        usage = "req_mention-bet",
        category = Category.FUN
)
@Requires({
        Permission.MESSAGE_MANAGE,
        Permission.MESSAGE_ADD_REACTION,
        Permission.MESSAGE_ATTACH_FILES
})
public class CrissCrossCommand implements Executable {

    @Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().isEmpty()) {
			channel.sendMessage(I18n.getString("err_no-user")).queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(message.getMentionedUsers().get(0).getId()) != null) {
			channel.sendMessage("❌ | Este usuário possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
			return;
		}

		Account uacc = Account.find(Account.class, author.getId());
        Account tacc = Account.find(Account.class, message.getMentionedUsers().get(0).getId());
        int bet = 0;
        if (args.length > 1 && StringUtils.isNumeric(args[1])) {
            bet = Integer.parseInt(args[1]);
            if (bet < 0) {
                channel.sendMessage(I18n.getString("err_invalid-credit-amount")).queue();
                return;
            } else if (uacc.getBalance() < bet) {
                channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
                return;
            } else if (tacc.getBalance() < bet) {
                channel.sendMessage(I18n.getString("err_insufficient-credits-target")).queue();
                return;
            }
        }

        if (Main.getInfo().gameInProgress(author.getId())) {
            channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
            return;
        } else if (Main.getInfo().gameInProgress(message.getMentionedUsers().get(0).getId())) {
            channel.sendMessage(I18n.getString("err_user-in-game")).queue();
            return;
        } else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
            channel.sendMessage(I18n.getString("err_cannot-play-with-yourself")).queue();
            return;
        }

        Main.getInfo().getConfirmationPending().put(author.getId(), true);
        int finalBet = bet;
		channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " você foi desafiado a uma partida de Jogo da Velha, deseja aceitar?" + (bet != 0 ? " (aposta: " + StringHelper.separate(bet) + " CR)" : ""))
				.queue(s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
							if (wrapper.getUser().getId().equals(message.getMentionedUsers().get(0).getId())) {
								Main.getInfo().getConfirmationPending().remove(author.getId());
								s.delete().queue(null, MiscHelper::doNothing);

								if (Main.getInfo().gameInProgress(wrapper.getUser().getId())) {
									channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
									return;
								} else if (Main.getInfo().gameInProgress(message.getMentionedUsers().get(0).getId())) {
									channel.sendMessage(I18n.getString("err_user-in-game")).queue();
									return;
								}

								s.delete().queue(null, MiscHelper::doNothing);
								Game t = new CrissCross(Main.getShiro(), channel, finalBet, author, message.getMentionedUsers().get(0));
								t.start();
							}
						}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
						u -> LogicHelper.equalsAny(u.getId(), author.getId(), message.getMentionedUsers().get(0).getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				));
    }
}
