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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.Stash;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

@Command(
		name = "deletar",
		aliases = {"delete"},
		usage = "req_ids",
		category = Category.MISC
)
public class CardDeleteCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você deve informar ao menos 1 ID.").queue();
			return;
		}

		int deleted = 0;
		for (String id : args) {
			if (!StringUtils.isNumeric(id)) {
				continue;
			}

			Stash s = StashDAO.getCard(Integer.parseInt(id));
			if (s == null || !s.getOwner().equals(author.getId())) {
				continue;
			}

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			switch (s.getType()) {
				case EVOGEAR -> {
					Deck dk = kp.getDeck();
					if (dk.checkEquipment(s.getCard(), channel)) continue;
				}
				case FIELD -> {
					Deck dk = kp.getDeck();
					if (dk.checkField(s.getCard(), channel)) continue;
				}
			}
			KawaiponDAO.saveKawaipon(kp);
			StashDAO.removeCard(s);
			deleted++;
		}

		channel.sendMessage("✅ | " + deleted + " cartas deletadas com sucesso!").queue();
	}
}
