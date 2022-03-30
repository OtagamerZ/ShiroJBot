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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

@Command(
		name = "statsheroi",
		aliases = {"herostats"},
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class HeroStatsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = KawaiponDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
			return;
		} else if (h.isUnavailable()) {
			channel.sendMessage("❌ | Este herói está em uma missão.").queue();
			return;
		}

		Main.getInfo().getConfirmationPending().put(h.getUid(), true);
		channel.sendMessageEmbeds(getEmbed(h)).queue(s ->
				Pages.buttonize(s, new LinkedHashMap<>() {{
							put(StringHelper.parseEmoji("\uD83C\uDDF8"), wrapper -> {
								if (h.getAvailableStatPoints() == 0) {
									channel.sendMessage("❌ | Você não tem mais pontos restantes.").queue();
									return;
								}

								h.getRawStats().addStr();
								s.editMessageEmbeds(getEmbed(h)).queue();
							});
							put(StringHelper.parseEmoji("\uD83C\uDDF7"), wrapper -> {
								if (h.getAvailableStatPoints() == 0) {
									channel.sendMessage("❌ | Você não tem mais pontos restantes.").queue();
									return;
								}

								h.getRawStats().addRes();
								s.editMessageEmbeds(getEmbed(h)).queue();
							});
							put(StringHelper.parseEmoji("\uD83C\uDDE6"), wrapper -> {
								if (h.getAvailableStatPoints() == 0) {
									channel.sendMessage("❌ | Você não tem mais pontos restantes.").queue();
									return;
								}

								h.getRawStats().addAgi();
								s.editMessageEmbeds(getEmbed(h)).queue();
							});
							put(StringHelper.parseEmoji("\uD83C\uDDFC"), wrapper -> {
								if (h.getAvailableStatPoints() == 0) {
									channel.sendMessage("❌ | Você não tem mais pontos restantes.").queue();
									return;
								}

								h.getRawStats().addWis();
								s.editMessageEmbeds(getEmbed(h)).queue();
							});
							put(StringHelper.parseEmoji("\uD83C\uDDE8"), wrapper -> {
								if (h.getAvailableStatPoints() == 0) {
									channel.sendMessage("❌ | Você não tem mais pontos restantes.").queue();
									return;
								}

								h.getRawStats().addCon();
								s.editMessageEmbeds(getEmbed(h)).queue();
							});
							put(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());

								KawaiponDAO.saveHero(h);

								s.delete()
										.flatMap(d -> wrapper.getMessage().delete())
										.flatMap(m -> channel.sendMessage("Herói salvo com sucesso!"))
										.queue(null, MiscHelper::doNothing);
							});
						}}, Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				));
	}

	private MessageEmbed getEmbed(Hero h) {
		Integer[] raw = h.getRawStats().getStats();
		Integer[] equip = h.getEquipStats().getStats();

		StringBuilder stats = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			switch (i) {
				case 0 -> stats.append("**S**TR: %s%s\n".formatted(
						raw[0], equip[0] != 0 ? " (" + StringHelper.sign(equip[0]) + ")" : ""
				));
				case 1 -> stats.append("**R**ES: %s%s\n".formatted(
						raw[1], equip[1] != 0 ? " (" + StringHelper.sign(equip[1]) + ")" : ""
				));
				case 2 -> stats.append("**A**GI: %s%s\n".formatted(
						raw[2], equip[2] != 0 ? " (" + StringHelper.sign(equip[2]) + ")" : ""
				));
				case 3 -> stats.append("**W**IS: %s%s\n".formatted(
						raw[3], equip[3] != 0 ? " (" + StringHelper.sign(equip[3]) + ")" : ""
				));
				case 4 -> stats.append("**C**ON: %s%s\n".formatted(
						raw[4], equip[4] != 0 ? " (" + StringHelper.sign(equip[4]) + ")" : ""
				));
			}
		}

		return new ColorlessEmbedBuilder()
				.setTitle("Atributos de " + h.getName())
				.addField("Pontos disponíveis: " + h.getAvailableStatPoints(), stats.toString(), true)
				.addField(
						"Atributos:",
						"""
								\\🗡️ Ataque: %s
								\\🛡️ Defesa: %s
								\\⚡ Esquiva: %s%%
								\\🪖 Bloqueio: %s%%

								\\🩸 HP: %s
								\\🧭 EP: %s
								\\🧪 MP: %s
																
								\\🎒 Equipamentos: %s
								\\⭐ Tier: %s
								""".formatted(
								h.getAtk(),
								h.getDef(),
								h.getDodge(),
								h.getBlock(),
								h.getMaxHp(),
								h.getMaxEnergy(),
								h.getMana(),
								h.getStats().calcInventoryCap(),
								h.getStats().calcEvoTierCap()
						),
						true
				)
				.build();
	}
}
