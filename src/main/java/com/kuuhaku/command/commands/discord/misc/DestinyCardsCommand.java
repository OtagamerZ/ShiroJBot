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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

public class DestinyCardsCommand extends Command {

	public DestinyCardsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public DestinyCardsCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public DestinyCardsCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public DestinyCardsCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
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

				kp.setDestinyDraw(Arrays.stream(values).map(i -> i - 1).toArray(Integer[]::new));

				channel.sendMessage("✅ | Cartas do destino definidas com sucesso.").queue();
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Posições inválidas, elas devem ser números inteiros de 1 à 30.").queue();
				return;
			}
		}

		KawaiponDAO.saveKawaipon(kp);
	}
}
