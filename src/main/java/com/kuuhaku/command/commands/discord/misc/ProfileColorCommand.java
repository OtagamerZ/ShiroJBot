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
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.List;

public class ProfileColorCommand extends Command {

	public ProfileColorCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ProfileColorCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ProfileColorCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ProfileColorCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		List<com.kuuhaku.model.persistent.Member> ms = MemberDAO.getMemberByMid(author.getId() + guild.getId());
		if (args.length < 1 || !args[0].contains("#") || !Helper.between(args[0].length(), 7, 8)) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-color")).queue();
			return;
		}

		if (Helper.equalsAny(args[0], "none", "reset", "resetar", "limpar")) {
			ms.forEach(m -> {
				m.setProfileColor("");
				MemberDAO.updateMemberConfigs(m);
			});
			channel.sendMessage("Cor de perfil restaurada ao padrão com sucesso!").queue();
			return;
		}

		try {
			ms.forEach(m -> {
				m.setProfileColor(args[0].toUpperCase());
				MemberDAO.updateMemberConfigs(m);
			});
			channel.sendMessage("Cor de perfil definida com sucesso!").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-color")).queue();
		}
	}
}
