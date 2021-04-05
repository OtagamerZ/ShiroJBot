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
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.CreditItem;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "lojac",
		aliases = {"shopc"},
		usage = "req_id",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class CreditStoreCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length < 1) {
			Helper.generateStore(
					author,
					channel,
					":coin: | Loja de créditos",
					"Esta loja possui vários artefatos que podem lhe dar uma vantagem rápida em certas coisas, a um preço justo claro!",
					Color.orange,
					List.of(CreditItem.values()),
					ci -> new MessageEmbed.Field("`ID: %s` | %s (%s créditos)".formatted(ci.ordinal(), ci.getName(), Helper.separate(ci.getPrice())), ci.getDesc(), true)
			).queue();
			return;
		}

		try {
			int i = Integer.parseInt(args[0]);

			if (!Helper.between(i, 0, CreditItem.values().length)) {
				channel.sendMessage("❌ | Esse item não existe.").queue();
				return;
			}

			CreditItem ci = CreditItem.values()[i];
			if (acc.getTotalBalance() < ci.getPrice()) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
				return;
			}

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Você está prestes a comprar o item `" + ci.getName() + "`, deseja confirmar?").queue(s ->
					Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
								if (ci.getEffect().apply(mb, channel, args)) {
									Account facc = AccountDAO.getAccount(author.getId());
									facc.consumeCredit(ci.getPrice(), CreditStoreCommand.class);
									AccountDAO.saveAccount(facc);

									s.delete().queue(null, Helper::doNothing);
									channel.sendMessage("✅ | Item comprado com sucesso!").queue();
								}
							}), true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> Main.getInfo().getConfirmationPending().remove(author.getId()))
			);
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | O ID deve ser um valor numérico.").queue();
		}
	}
}