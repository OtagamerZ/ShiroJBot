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

package com.kuuhaku.command.commands.discord.clan;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Clan;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

public class CreateClanCommand extends Command {

	public CreateClanCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public CreateClanCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public CreateClanCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public CreateClanCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (ClanDAO.isMember(author.getId())) {
			channel.sendMessage("❌ | Você já possui um clã.").queue();
			return;
		}

		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa informar um nome para o clã.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		if (acc.getBalance() + acc.getVBalance() < 10000) {
			channel.sendMessage("❌ | Você precisa de 10000 créditos para poder criar um clã.").queue();
			return;
		}

		String name = String.join(" ", args);
		if (name.length() > 20) {
			channel.sendMessage("❌ | O nome deve ser menor que 20 caracteres.").queue();
			return;
		} else if (!StringUtils.isAlphanumericSpace(name)) {
			channel.sendMessage("❌ | O nome deve conter apenas letras, números ou espaços.").queue();
			return;
		} else if (ClanDAO.getClan(name) != null) {
			channel.sendMessage("❌ | Já existe um clã com esse nome.").queue();
			return;
		}

		Clan c = new Clan(name, author.getId());
		ClanDAO.saveClan(c);

		channel.sendMessage("✅ | Clã " + name + " criado com sucesso.").queue();
	}
}
