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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Perk;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "heroi",
		aliases = {"hero"},
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class MyHeroCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = KawaiponDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
			return;
		} else if (h.getImage() == null) {
			channel.sendMessage("❌ | Seu herói não possui uma imagem.").queue();
			return;
		}

		List<String> perks = h.getPerks().stream().map(Perk::toString).collect(Collectors.toList());
		for (int i = 0; i < h.getAvailablePerks(); i++) {
			perks.add("`Perk disponível`");
		}

		List<String> equips = new ArrayList<>(h.getInventoryNames());
		for (int i = 0; i < h.getInventoryCap(); i++) {
			equips.add("`Slot disponível (tier " + h.getStats().calcEvoTierCap() + ")`");
		}

		double healModif = 1;
		for (Perk perk : h.getPerks()) {
			healModif *= switch (perk) {
				case OPTIMISTIC -> 0.5;
				case PESSIMISTIC -> 1.5;
				default -> 1;
			};
		}

		Integer[] raw = h.getRawStats().getStats();
		Integer[] equip = h.getEquipStats().getStats();

		StringBuilder stats = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			switch (i) {
				case 0 -> stats.append("**S**TR: %s%s\n".formatted(
						raw[0], equip[0] != 0 ? " (" + Helper.sign(equip[0]) + ")" : ""
				));
				case 1 -> stats.append("**R**ES: %s%s\n".formatted(
						raw[1], equip[1] != 0 ? " (" + Helper.sign(equip[1]) + ")" : ""
				));
				case 2 -> stats.append("**A**GI: %s%s\n".formatted(
						raw[2], equip[2] != 0 ? " (" + Helper.sign(equip[2]) + ")" : ""
				));
				case 3 -> stats.append("**W**IS: %s%s\n".formatted(
						raw[3], equip[3] != 0 ? " (" + Helper.sign(equip[3]) + ")" : ""
				));
				case 4 -> stats.append("**C**ON: %s%s\n".formatted(
						raw[4], equip[4] != 0 ? " (" + Helper.sign(equip[4]) + ")" : ""
				));
			}
		}

		int hours = (int) Math.ceil((10 - Helper.prcnt(h.getHp(), h.getMaxHp()) * 10) * healModif);
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Herói " + h.getName())
				.addField(":chart_with_upwards_trend: | Nível: " + h.getLevel(), """
						XP: %s
						HP: %s/%s%s
						EP: %s/%s
						""".formatted(
						h.getXp() + "/" + h.getXpToNext(),
						h.getHp(),
						h.getMaxHp(),
						h.getHp() < h.getMaxHp() ? "\n`recuperação total em " + hours + " hora" + (hours != 1 ? "s" : "") + "`" : "",
						h.getEnergy(),
						h.getMaxEnergy()
				), true)
				.addField(":bar_chart: | Atributos:", stats.toString(), true)
				.addField(":books: | Perks:", String.join("\n", perks), true)
				.addField(":books: | Equipamentos:", String.join("\n", equips), true)
				.setImage("attachment://hero.png");

		if (h.isQuesting())
			eb.setFooter("\uD83E\uDDED | " + h.getQuest() + ": " + Helper.toStringDuration(h.getQuestEnd() - System.currentTimeMillis()));

		Champion c = h.toChampion();
		if (h.getHp() == 0) c.setStun(1);

		channel.sendMessageEmbeds(eb.build())
				.addFile(Helper.getBytes(c.drawCard(false), "png"), "hero.png")
				.queue();
	}
}
