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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "nomedocla",
		aliases = {"clanname"},
		usage = "req_name",
		category = Category.CLAN
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class ClanChangeNameCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (c.getMembers().get(author.getId()).ordinal() != 0) {
			channel.sendMessage("❌ | Apenas o líder pode mudar o nome do clã.").queue();
			return;
		} else if (c.getVault() < 100000) {
			channel.sendMessage("❌ | O cofre do clã não possui créditos suficientes.").queue();
			return;
		}

		String name = StringUtils.normalizeSpace(String.join(" ", args));
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

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Tem certeza que deseja mudar o nome do clã de `" + c.getName() + "` para `" + name + "` por 100.000 créditos?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());

							c.changeName(author, name);
							ClanDAO.saveClan(c);

							s.delete().flatMap(d -> channel.sendMessage("✅ | Nome alterado com sucesso.")).queue();
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				));
	}
}
