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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.helpers.LogicHelper;
import com.kuuhaku.utils.helpers.MathHelper;
import net.dv8tion.jda.api.entities.*;

import java.util.Locale;

@Command(
		name = "cordoperfil",
		aliases = {"profilecolor", "cp", "pc"},
		usage = "req_color",
		category = Category.MISC
)
public class ProfileColorCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = Account.find(Account.class, author.getId());
		if (args.length == 0) {
			channel.sendMessage("❌ | O primeiro argumento deve ser uma cor em formato hexadecimal (#RRGGBB) ou `reset`.").queue();
			return;
		} else if (LogicHelper.equalsAny(args[0], "none", "reset", "resetar", "limpar")) {
			acc.setProfileColor("");
			acc.save();
			channel.sendMessage("✅ | Cor de perfil restaurada ao padrão com sucesso!").queue();
			return;
		} else if (!args[0].contains("#") || !MathHelper.between(args[0].length(), 7, 8)) {
			channel.sendMessage(I18n.getString("err_invalid-color")).queue();
			return;
		}

		try {
			acc.setProfileColor(args[0].toUpperCase(Locale.ROOT));
			acc.save();
			channel.sendMessage("✅ | Cor de perfil definida com sucesso!").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage(I18n.getString("err_invalid-color")).queue();
		}
	}
}
