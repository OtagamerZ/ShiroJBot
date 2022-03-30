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
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.CreditItem;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
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
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = Account.find(Account.class, author.getId());

		if (args.length < 1) {
			MiscHelper.generateStore(
					author,
					channel,
					":coin: | Loja de CR",
					"Esta loja possui vários artefatos que podem lhe dar uma vantagem rápida em certas coisas, a um preço justo claro!",
					Color.orange,
					List.of(CreditItem.values()),
					ci -> new MessageEmbed.Field("`ID: %s` | %s (%s CR)".formatted(ci.ordinal(), ci.getName(), StringHelper.separate(ci.getPrice())), ci.getDesc(), true)
			).queue();
			return;
		}

		try {
			int i = Integer.parseInt(args[0]);

			if (!MathHelper.between(i, 0, CreditItem.values().length)) {
				channel.sendMessage("❌ | Esse item não existe.").queue();
				return;
			}

			CreditItem ci = CreditItem.values()[i];
			if (acc.getTotalBalance() < ci.getPrice()) {
				channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
				return;
			}

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Você está prestes a comprar o item `" + ci.getName() + "`, deseja confirmar?").queue(s ->
					Pages.buttonize(s, Collections.singletonMap(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());

								if (ci.getEffect().apply(wrapper.getMember(), channel, args)) {
									Account facc = Account.find(Account.class, author.getId());
									facc.consumeCredit(ci.getPrice(), CreditStoreCommand.class);
									facc.save();

									s.delete().mapToResult().flatMap(d -> channel.sendMessage("✅ | Item comprado com sucesso!")).queue();
								}
							}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> Main.getInfo().getConfirmationPending().remove(author.getId()))
			);
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | O ID deve ser um valor numérico.").queue();
		}
	}
}