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

package com.kuuhaku.command.commands.discord.hero;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "desequiparheroi",
		aliases = {"unequiphero"},
		usage = "req_card",
		category = Category.MISC
)
public class UnequipHeroCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = KawaiponDAO.getHero(author.getId());
		Deck dk = KawaiponDAO.getDeck(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
			return;
		} else if (h.isUnavailable()) {
			channel.sendMessage("❌ | Este herói está em uma missão.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar uma carta.").queue();
			return;
		}

		String name = args[0];
		Evogear e = Evogear.getEvogear(name);
		if (e == null) {
			channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + StringHelper.didYouMean(name, Evogear.getEvogears().stream().map(d -> d.getCard().getId()).toList()) + "`?").queue();
			return;
		} else if (!h.getInventory().contains(e)) {
			channel.sendMessage("❌ | Você não pode desequipar uma carta que não possui!").queue();
			return;
		} else if (dk.checkEquipment(e, channel)) return;

		dk.addEquipment(e);
		h.getInventory().remove(e);

		KawaiponDAO.saveDeck(dk);
		KawaiponDAO.saveHero(h);

		channel.sendMessage("Desequipado com sucesso!").queue();
	}
}
