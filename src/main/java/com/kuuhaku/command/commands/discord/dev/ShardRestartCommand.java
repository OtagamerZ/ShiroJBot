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

package com.kuuhaku.command.commands.discord.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.List;

@Command(
		name = "rshard",
		aliases = {"reiniciar", "reboot", "restart"},
		usage = "req_id",
		category = Category.DEV
)
public class ShardRestartCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
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

		channel.sendMessage("Reiniciando Shard com ID %s...".formatted(id)).queue(m -> {
			Main.getShiroShards().restart(id);
			List<JDA> shards = Main.getShiroShards().getShards().stream()
					.sorted(Comparator.comparingInt(s -> s.getShardInfo().getShardId()))
					.filter(s -> s.getStatus() != JDA.Status.CONNECTED)
					.toList();
			for (JDA shard : shards) {
				try {
					shard.awaitReady();
					shard.getPresence().setActivity(Main.getRandomActivity());

					Helper.logger(Main.class).info("Shard " + id + " pronto!");
				} catch (InterruptedException e) {
					Helper.logger(Main.class).error("Erro ao inicializar shard " + id + ": " + e);
				}
			}

			m.editMessage("Shard reiniciado com sucesso.").queue();
		});
	}
}