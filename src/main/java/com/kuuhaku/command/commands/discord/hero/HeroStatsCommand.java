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
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "statsheroi",
		aliases = {"herostats"},
		category = Category.SUPPORT
)
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class HeroStatsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = CardDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui um herói.").queue();
			return;
		}

		channel.sendMessageEmbeds(getEmbed(h)).queue(s ->
				Pages.buttonize(s, Map.of(
						"\uD83C\uDDF8", (mb, ms) -> {
							if (h.getAvailableStatPoints() <= 0) {
								channel.sendMessage("❌ | Você não tem mais pontos restantes").queue();
								return;
							}

							h.getStats().addStr();
                            s.editMessageEmbeds(getEmbed(h)).queue();
						},
						"\uD83C\uDDF7", (mb, ms) -> {
							if (h.getAvailableStatPoints() <= 0) {
								channel.sendMessage("❌ | Você não tem mais pontos restantes").queue();
								return;
							}

                            h.getStats().addRes();
                            s.editMessageEmbeds(getEmbed(h)).queue();
						},
						"\uD83C\uDDE6", (mb, ms) -> {
							if (h.getAvailableStatPoints() <= 0) {
								channel.sendMessage("❌ | Você não tem mais pontos restantes").queue();
								return;
							}

                            h.getStats().addAgi();
                            s.editMessageEmbeds(getEmbed(h)).queue();
						},
						"\uD83C\uDDFC", (mb, ms) -> {
							if (h.getAvailableStatPoints() <= 0) {
								channel.sendMessage("❌ | Você não tem mais pontos restantes").queue();
								return;
							}

                            h.getStats().addWis();
                            s.editMessageEmbeds(getEmbed(h)).queue();
						},
						"\uD83C\uDDE8", (mb, ms) -> {
							if (h.getAvailableStatPoints() <= 0) {
								channel.sendMessage("❌ | Você não tem mais pontos restantes").queue();
								return;
							}

                            h.getStats().addCon();
                            s.editMessageEmbeds(getEmbed(h)).queue();
						},
						Helper.ACCEPT, (mb, ms) -> {
							channel.sendMessage("Herói salvo com sucesso!").queue();
							CardDAO.saveHero(h);
						}
				), true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}

	private MessageEmbed getEmbed(Hero h) {
		return new ColorlessEmbedBuilder()
				.setTitle("Atributos de " + h.getName())
				.setDescription("""
						Pontos disponíveis: %s

						STR: %s | `Aumenta ataque, HP e custo`
						RES: %s | `Aumenta defesa, HP e custo`
						AGI: %s | `Aumenta esquiva, ataque, defesa e custo`
						WIS: %s | `Reduz custo`
						CON: %s | `Aumenta HP e custo e reduz esquiva`
						"""
						.formatted(h.getAvailableStatPoints(),
                                h.getStats().getStr(),
                                h.getStats().getRes(),
                                h.getStats().getAgi(),
                                h.getStats().getWis(),
                                h.getStats().getCon()
                        )
				).build();
	}
}
