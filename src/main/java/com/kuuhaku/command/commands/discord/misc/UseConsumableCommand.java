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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.Consumable;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.ConsumableShop;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class UseConsumableCommand extends Command {

	public UseConsumableCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public UseConsumableCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public UseConsumableCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public UseConsumableCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length < 1) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();
			eb.setTitle(":test_tube: | Inventário");
			ConsumableShop.getAvailable().forEach((k, v) -> eb.addField("`" + k + "` | " + v.getName(), "Possui: " + acc.getBuffs().getOrDefault(k, 0), false));

			channel.sendMessage(eb.build()).queue();
			return;
		}

		if (acc.getBuffs().getOrDefault(args[0], 0) <= 0) {
			channel.sendMessage("❌ | Você não possui esse item.").queue();
			return;
		}

		Consumable c = ConsumableShop.getAvailable().get(args[0]);
		c.getEffect().accept(member, (TextChannel) channel, message);

		acc.removeBuff(args[0]);
		AccountDAO.saveAccount(acc);
	}
}