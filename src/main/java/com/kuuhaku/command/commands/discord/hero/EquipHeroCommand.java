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
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "equiparheroi",
		aliases = {"equiphero"},
		usage = "req_card",
		category = Category.MISC
)
public class EquipHeroCommand implements Executable {

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
		} else if (h.getInventoryCap() == 0) {
			channel.sendMessage("❌ | Seu herói não pode equipar mais nada.").queue();
			return;
		}

		String name = args[0];
		Equipment e = CardDAO.getEquipment(name);
		if (e == null) {
			channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + Helper.didYouMean(name, CardDAO.getAllEquipmentNames().toArray(String[]::new)) + "`?").queue();
			return;
		} else if (!dk.getEquipments().contains(e)) {
			channel.sendMessage("❌ | Você não pode equipar uma carta que não possui!").queue();
			return;
		} else if (h.getInventory().contains(e)) {
			channel.sendMessage("❌ | Seu herói já possui esse equipamento!").queue();
			return;
		} else if (e.isSpell()) {
			channel.sendMessage("❌ | Você não pode equipar magias!").queue();
			return;
		} else if (e.getTier() > h.getStats().calcEvoTierCap()) {
			channel.sendMessage("❌ | Seu herói não é forte o suficiente para equipar esse equipamento!").queue();
			return;
		}

		dk.removeEquipment(e);
		h.getInventory().add(e);

		KawaiponDAO.saveDeck(dk);
		KawaiponDAO.saveHero(h);

		channel.sendMessage("Equipado com sucesso!").queue();
	}
}
