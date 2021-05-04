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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;
import java.util.stream.Collectors;

@Command(
		name = "destino",
		aliases = {"destiny", "dest"},
		usage = "req_positions",
		category = Category.MISC
)
public class DestinyCardsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		if (args.length == 0) {
			kp.setDestinyDraw(null);

			channel.sendMessage("✅ | Cartas do destino limpas com sucesso.").queue();
		} else {
			String[] inds = Arrays.stream(args[0].split(",")).distinct().toArray(String[]::new);

			if (inds.length != 4) {
				channel.sendMessage("❌ | Você precisa digitar 4 posíções distintas do deck (separadas por vírgula) para serem usadas no saque do destino.").queue();
				return;
			}

			try {
				Integer[] values = Arrays.stream(inds).map(Integer::parseInt).toArray(Integer[]::new);
				for (Integer value : values) {
					if (!Helper.between(value, 1, 37)) {
						channel.sendMessage("❌ | Posições inválidas, elas devem ser números inteiros entre 1 e 36.").queue();
						return;
					}
				}

				kp.setDestinyDraw(Arrays.stream(values).map(i -> i - 1).collect(Collectors.toList()));

				channel.sendMessage("✅ | Cartas do destino definidas com sucesso.").queue();
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Posições inválidas, elas devem ser números inteiros de 1 à 30.").queue();
				return;
			}
		}

		KawaiponDAO.saveKawaipon(kp);
	}
}
