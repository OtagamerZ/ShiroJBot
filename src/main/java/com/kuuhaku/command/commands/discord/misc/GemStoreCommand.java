/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.enums.GemItem;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "lojag",
		aliases = {"shopg"},
		usage = "req_id",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class GemStoreCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length < 1) {
			Helper.generateStore(
					author,
					channel,
					":diamonds: | Loja de gemas",
					"""
							Gemas podem ser obtidas ao resgatar um acúmulo de 7 votos seguidos com o comando `%sresgatar`.
							     
							Muito obrigada por me apoiar!
							""".formatted(prefix),
					Color.red,
					List.of(GemItem.values()),
					ci -> new MessageEmbed.Field("`ID: %s` | %s (%s gemas)".formatted(ci.ordinal(), ci.getName(), Helper.separate(ci.getPrice())), ci.getDesc(), true)
			).queue();
			return;
		}

		try {
			int i = Integer.parseInt(args[0]);

			if (!Helper.between(i, 0, GemItem.values().length)) {
				channel.sendMessage("❌ | Esse item não existe.").queue();
				return;
			}

			GemItem gi = GemItem.values()[i];
			if (acc.getGems() < gi.getPrice()) {
				channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
				return;
			}

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Você está prestes a comprar o item `" + gi.getName() + "`, deseja confirmar?").queue(s ->
					Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());

								if (gi.getEffect().apply(mb, channel, args)) {
									Account facc = AccountDAO.getAccount(author.getId());
									facc.removeGem(gi.getPrice());
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