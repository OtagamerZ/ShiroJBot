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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.handlers.games.kawaigotchi.enums.Tier;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.GuildBuff;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ServerBuff;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

public class MyBuffsCommand extends Command {

	public MyBuffsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public MyBuffsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public MyBuffsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public MyBuffsCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Kawaigotchi kg = KGotchiDAO.getKawaigotchi(author.getId());

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		boolean exceed = Main.getInfo().getWinner().equals(ExceedDAO.getExceed(author.getId()));
		boolean waifu = guild.getMembers().stream().map(net.dv8tion.jda.api.entities.Member::getId).collect(Collectors.toList()).contains(com.kuuhaku.model.persistent.Member.getWaifu(author.getId()));
		boolean kgotchi = kg != null;

		eb.setTitle(":level_slider: Modificadores ativos");

		if (exceed) eb.addField("Seu Exceed foi vitorioso", "+200% XP ganho", false);
		if (waifu)
			eb.addField("Você está no mesmo servidor que sua waifu/husbando", "+" + (int) (WaifuDAO.getMultiplier(author).getMult() * 100 - 100) + "% XP ganho", false);
		if (kgotchi)
			if (kg.isAlive() && kg.getTier() != Tier.CHILD)
				eb.addField("Seu kawaigotchi é um " + kg.getTier().toString().toLowerCase(), "+" + (int) (kg.getTier().getUserXpMult() * 100 - 100) + "% XP ganho", false);
			else if (!kg.isAlive())
				eb.addField("Seu kawaigotchi morreu", "-20% XP ganho", false);

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
				String until = ZonedDateTime.ofInstant(Instant.ofEpochMilli(b.getAcquiredAt()), ZoneId.of("GMT-3"))
						.plusDays(b.getTime())
						.format(Helper.onlyDate);
				switch (b.getId()) {
					case 1 -> eb.addField("Melhoria de servidor (XP)", "+" + (int) ((b.getMult() * 100) - 100) + "% XP ganho (até " + until + ")", false);
					case 2 -> {
						if (isUltimate)
							eb.addField("Melhoria de servidor (cartas)", "Bônus ultimate, todas as mensagens tem 100% de chance de spawn de cartas (1 minuto)", false);
						else
							eb.addField("Melhoria de servidor (cartas)", "+" + (int) ((b.getMult() * 100) - 100) + "% chance de spawn de cartas (até " + until + ")", false);
					}
					case 3 -> {
						if (isUltimate)
							eb.addField("Melhoria de servidor (drops)", "Bônus ultimate, todas as mensagens tem 100% de chance de spawn de drops (1 minuto)", false);
						else
							eb.addField("Melhoria de servidor (drops)", "+" + (int) ((b.getMult() * 100) - 100) + "% chance de spawn de drops (até " + until + ")", false);
					}
					case 4 -> {
						if (isUltimate)
							eb.addField("Melhoria de servidor (cromadas)", "Bônus ultimate, todas as cartas tem 100% de chance de serem cromadas (1 minuto)", false);
						else
							eb.addField("Melhoria de servidor (cromadas)", "+" + (int) ((b.getMult() * 100) - 100) + "% chance de spawn de cartas cromadas (até " + until + ")", false);
					}
				}
			}
		}

		eb.setColor(Helper.getRandomColor());

		channel.sendMessage(eb.build()).queue();
	}
}
