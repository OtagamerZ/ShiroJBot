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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.Slashed;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.WaifuDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.guild.Buff;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Command(
		name = "buffs",
		aliases = {"meusbuffs", "modifiers", "modifs", "boosts"},
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
@SlashGroup("meus")
@SlashCommand(name = "buffs")
public class MyBuffsCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(":level_slider: Modificadores ativos")
				.setColor(Helper.getRandomColor());

		boolean waifu = guild.getMembers().stream().map(Member::getId).toList().contains(com.kuuhaku.model.persistent.Member.getWaifu(author.getId()));

		if (waifu)
			eb.addField("Você está no mesmo servidor que sua waifu/husbando", "+" + Helper.roundToString(WaifuDAO.getMultiplier(author.getId()).getMult() * 100 - 100, 0) + "% XP ganho", false);

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		float progress = kp.getCards().size() / (CardDAO.getTotalCards() * 2f);

		if (progress >= 1)
			eb.addField("Coleção de cartas (100%)", "+100% XP ganho", false);
		else if (progress >= 0.75)
			eb.addField("Coleção de cartas (75%)", "+75% XP ganho", false);
		else if (progress >= 0.5)
			eb.addField("Coleção de cartas (50%)", "+50% XP ganho", false);
		else if (progress >= 0.25)
			eb.addField("Coleção de cartas (25%)", "+25% XP ganho", false);

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		if (!gc.getBuffs().isEmpty()) {
			for (Buff b : gc.getBuffs()) {
				String until = Helper.TIMESTAMP.formatted((b.getAcquiredAt().plus(b.getTime(), ChronoUnit.MILLIS).toEpochSecond()));
				String chance = Helper.roundToString(b.getMultiplier() * 100 - 100, 0) + "%";
				switch (b.getType()) {
					case XP -> eb.addField("Melhoria de servidor (XP)", "+" + chance + " ganho de XP (expira " + until + ")", false);
					case CARD -> eb.addField("Melhoria de servidor (cartas)", "+" + chance + " chance de aparecer cartas (expira " + until + ")", false);
					case DROP -> eb.addField("Melhoria de servidor (drops)", "+" + chance + " chance de aparecer drops (expira " + until + ")", false);
					case FOIL -> eb.addField("Melhoria de servidor (cromadas)", "+" + chance + " chance de cartas serem cromadas (expira " + until + ")", false);
				}
			}
		}

		if (gc.isPartner())
			eb.addField("Servidor parceiro", "+20% buff global (XP/cartas/drops/cromadas)", false);

		channel.sendMessageEmbeds(eb.build()).queue();
	}
}
