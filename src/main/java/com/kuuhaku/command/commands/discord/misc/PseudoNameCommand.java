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
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class PseudoNameCommand extends Command {

	public PseudoNameCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PseudoNameCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PseudoNameCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PseudoNameCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		com.kuuhaku.model.persistent.Member mb = MemberDAO.getMemberById(author.getId() + guild.getId());
		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa digitar um nome ou `reset`.").queue();
			return;
		} else if (Helper.equalsAny(args[0], "reset", "limpar")) {
			mb.setPseudoName("");
			MemberDAO.updateMemberConfigs(mb);
			channel.sendMessage("✅ | Pseudônimo limpo com sucesso!").queue();
			return;
		} else if (String.join(" ", args).length() > 32) {
			channel.sendMessage("❌ | Por favor escolha um nome mais curto.").queue();
			return;
		}

		mb.setPseudoName(String.join(" ", args));
		MemberDAO.updateMemberConfigs(mb);
		channel.sendMessage("✅ | Pseudônimo definido com sucesso!").queue();
	}
}
