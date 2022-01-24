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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "emprestimo",
		aliases = {"emp", "loan"},
		usage = "req_loan",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class LoanCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setTitle(":bank: | Empréstimo de CR");
			eb.setThumbnail("https://image.flaticon.com/icons/png/512/1462/1462438.png");
			eb.setDescription("""
					Está precisando de CR rápidos? Estão aparecendo muitas cartas que você deseja obter?
					Não se preocupe, nós podemos resolver!
					     
					Usando este comando você pode contratar um ~~agiota~~ empréstimo de CR e ter a possibilidade de pagar a dívida mais tarde.
					""");
			eb.setFooter("A cada dia sua dívida aumentará em 3% (até no máximo 100%), todo CR que você ganhar reduzirá a dívida.");

			channel.sendMessageEmbeds(eb.build()).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | O valor deve ser numérico.").queue();
			return;
		}

		int loan = Integer.parseInt(args[0]);
		if (!Helper.between(loan, 1000, 250_001)) {
			channel.sendMessage("❌ | O empréstimo deve ser um valor entre 1.000 e 250.000 CR.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		if (acc.getLoan() > 0) {
			channel.sendMessage("❌ | Você ainda não terminou de pagar seu último empréstimo.").queue();
			return;
		}

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Você está prestes a obter __**" + Helper.separate(loan) + " CR**__ a um juros de __3% a.d.__ (__max: 100%__), deseja confirmar?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());
							Account finalAcc = AccountDAO.getAccount(author.getId());
							finalAcc.addLoan(loan);

							AccountDAO.saveAccount(finalAcc);
							s.delete().flatMap(d -> channel.sendMessage("Obrigada por ser mais um cliente do Shiro Empréstimos LTDA!")).queue();
						}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				));
	}

}
