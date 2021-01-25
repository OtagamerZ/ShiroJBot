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
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class BinaryCommand extends Command {

	public BinaryCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BinaryCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BinaryCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BinaryCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		String text = String.join(" ", args);
		try {
			byte[] bytes = text.getBytes();
			StringBuilder binary = new StringBuilder();
			for (byte b : bytes) {
				int val = b;
				for (int i = 0; i < 8; i++) {
					binary.append((val & 128) == 0 ? 0 : 1);
					val <<= 1;
				}
				binary.append(' ');
			}
			channel.sendMessage(":1234: `" + binary.toString() + "`").queue();
		} catch (IllegalStateException | IllegalArgumentException e) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_message-too-long")).queue();
        }
	}
}
