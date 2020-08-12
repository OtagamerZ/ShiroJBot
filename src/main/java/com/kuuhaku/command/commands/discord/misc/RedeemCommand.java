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
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RedeemCommand extends Command {

	public RedeemCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public RedeemCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public RedeemCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public RedeemCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (acc.getStreak() < 7) {
			channel.sendMessage(":x: | Você não chegou no acúmulo máximo de votos ainda (" + acc.getStreak() + " de 7)").queue();
			return;
		}

		channel.sendMessage("Deseja realmente trocar seu acúmulo de 7 votos por uma gema?").queue(s ->
				Pages.buttonize(s, Map.of(Helper.ACCEPT, (m, ms) -> {
					if (m.getId().equals(author.getId())) {
						acc.setStreak(0);
						acc.addGem();
						AccountDAO.saveAccount(acc);

						s.delete().queue();
						channel.sendMessage("Gema adquirida com sucesso! Use `" + prefix + "vip` para ver a loja de gemas.").queue();
					}
				}), true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
