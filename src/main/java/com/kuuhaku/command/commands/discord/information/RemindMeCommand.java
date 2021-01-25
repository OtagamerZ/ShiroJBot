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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.persistent.Account;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class RemindMeCommand extends Command {

	public RemindMeCommand(@NonNls String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public RemindMeCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public RemindMeCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public RemindMeCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		if (acc.shouldRemind()) {
			channel.sendMessage(":mobile_phone_off: | Não irei mais te avisar quando você puder votar novamente!").queue();
			acc.setRemind(false);
		} else {
			channel.sendMessage(":vibration_mode: | Agora irei te avisar quando você puder votar novamente!").queue();
			acc.setRemind(true);
		}
		AccountDAO.saveAccount(acc);
	}
}
