/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.guild.GuildBuff;
import com.kuuhaku.model.persistent.guild.ServerBuff;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.stream.Collectors;

@Command(
		name = "buffs",
		aliases = {"meusbuffs", "modifiers", "modifs", "boosts"},
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class MyBuffsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		boolean exceed = Main.getInfo().getWinner().equals(ExceedDAO.getExceed(author.getId()));
		boolean waifu = guild.getMembers().stream().map(net.dv8tion.jda.api.entities.Member::getId).collect(Collectors.toList()).contains(com.kuuhaku.model.persistent.Member.getWaifu(author.getId()));

		eb.setTitle(":level_slider: Modificadores ativos");

		if (exceed) eb.addField("Seu Exceed foi vitorioso", "+200% XP ganho", false);
		if (waifu)
			eb.addField("Você está no mesmo servidor que sua waifu/husbando", "+" + Helper.roundToString(WaifuDAO.getMultiplier(author.getId()).getMult() * 100 - 100, 0) + "% XP ganho", false);

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 1)
			eb.addField("Coleção de cartas (100%)", "+100% XP ganho", false);
		else if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 0.75)
			eb.addField("Coleção de cartas (75%)", "+75% XP ganho", false);
		else if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 0.5)
			eb.addField("Coleção de cartas (50%)", "+50% XP ganho", false);
		else if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 0.25)
			eb.addField("Coleção de cartas (25%)", "+25% XP ganho", false);

		GuildBuff gb = GuildBuffDAO.getBuffs(guild.getId());
		if (gb.getBuffs().size() > 0) {
			for (ServerBuff b : gb.getBuffs()) {
				boolean isUltimate = b.getTier() == 4;
				String until = "<t:" + (b.getAcquiredAt() + b.getTime()) + ">";

				String chance = Helper.roundToString(b.getMult() * 100 - 100, 0) + "%";
				switch (b.getType()) {
					case XP -> eb.addField("Melhoria de servidor (XP)", "+" + chance + " XP ganho (até " + until + ")", false);
					case CARD -> {
						if (isUltimate)
							eb.addField("Melhoria de servidor (cartas)", "Bônus ultimate, todas as mensagens tem 100% de chance de spawn de cartas (1 minuto)", false);
						else
							eb.addField("Melhoria de servidor (cartas)", "+" + chance + " chance de spawn de cartas (até " + until + ")", false);
					}
					case DROP -> {
						if (isUltimate)
							eb.addField("Melhoria de servidor (drops)", "Bônus ultimate, todas as mensagens tem 100% de chance de spawn de drops (1 minuto)", false);
						else
							eb.addField("Melhoria de servidor (drops)", "+" + chance + " chance de spawn de drops (até " + until + ")", false);
					}
					case FOIL -> {
						if (isUltimate)
							eb.addField("Melhoria de servidor (cromadas)", "Bônus ultimate, todas as cartas tem 100% de chance de serem cromadas (1 minuto)", false);
						else
							eb.addField("Melhoria de servidor (cromadas)", "+" + chance + " chance de spawn de cartas cromadas (até " + until + ")", false);
					}
				}
			}
		}

		eb.setColor(Helper.getRandomColor());

		channel.sendMessageEmbeds(eb.build()).queue();
	}
}
