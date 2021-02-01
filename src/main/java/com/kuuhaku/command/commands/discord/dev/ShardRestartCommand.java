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

package com.kuuhaku.command.commands.discord.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

@Command(
		name = "rshard",
		aliases = {"reiniciar", "reboot"},
		usage = "req_id",
		category = Category.DEV
)
public class ShardRestartCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário informar o ID do Shard a ser reiniciado.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | O ID deve ser numérico.").queue();
			return;
		}

		int id = Integer.parseInt(args[0]);

		if (Main.getShiroShards().getShardById(id) == null) {
			channel.sendMessage("❌ | Não existe nenhum Shard com esse ID.").queue();
			return;
		}

		if (author.getId().equals(ShiroInfo.getNiiChan())) {
			channel.sendMessage("Reiniciando Shard com ID %s...".formatted(id)).queue();
		}

		Main.getShiroShards().restart(id);
	}
}
