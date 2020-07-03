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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.WaifuDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.handlers.games.kawaigotchi.enums.Tier;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

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
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Kawaigotchi kg = KGotchiDAO.getKawaigotchi(author.getId());

		EmbedBuilder eb = new EmbedBuilder();

		boolean exceed = Main.getInfo().getWinner().equals(ExceedDAO.getExceed(author.getId()));
		boolean waifu = guild.getMembers().stream().map(net.dv8tion.jda.api.entities.Member::getId).collect(Collectors.toList()).contains(com.kuuhaku.model.persistent.Member.getWaifu(author));
		boolean kgotchi = kg != null;

		eb.setTitle(":level_slider: Seus modificadores de XP");

		if (exceed) eb.addField("Seu exceed foi vitorioso", "+200% XP ganho", false);
		if (waifu)
			eb.addField("Você está no mesmo servidor que sua waifu/husbando", "+" + (int) (WaifuDAO.getMultiplier(author).getMult() * 100 - 100) + "% XP ganho", false);
		if (kgotchi)
			if (kg.isAlive() && kg.getTier() != Tier.CHILD)
				eb.addField("Seu kawaigotchi é um " + kg.getTier().toString().toLowerCase(), "+" + (int) (kg.getTier().getUserXpMult() * 100 - 100) + "% XP ganho", false);
			else eb.addField("Seu kawaigotchi morreu", "-20% XP ganho", false);

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		if (kp.getCards().size() / (float) CardDAO.totalCards() >= 1)
			eb.addField("Coleção de cartas (100%)", "+50% XP ganho", false);
		else if (kp.getCards().size() / (float) CardDAO.totalCards() >= 0.75)
			eb.addField("Coleção de cartas (75%)", "+37% XP ganho", false);
		else if (kp.getCards().size() / (float) CardDAO.totalCards() >= 0.5)
			eb.addField("Coleção de cartas (50%)", "+25% XP ganho", false);
		else if (kp.getCards().size() / (float) CardDAO.totalCards() >= 0.25)
			eb.addField("Coleção de cartas (25%)", "+12% XP ganho", false);

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		if (gc.getBuffs().size() > 0) {
			gc.getBuffs().forEach(b -> {
				switch (b.getId()) {
					case 1:
						eb.addField("", "+" + (100 - (b.getMult() * 100)) + "% XP ganho (" + b.getTime() + " dias)", false);
						break;
					case 2:
						eb.addField("", "+" + (100 - (b.getMult() * 100)) + "% chance de spawn de cartas (" + b.getTime() + " dias)", false);
						break;
					case 3:
						eb.addField("", "+" + (100 - (b.getMult() * 100)) + "% chance de spawn de drops (" + b.getTime() + " dias)", false);
						break;
					case 4:
						eb.addField("", "+" + (100 - (b.getMult() * 100)) + "% chance de spawn de cartas cromadas (" + b.getTime() + " dias)", false);
						break;
				}
			});
		}

		eb.setColor(Helper.getRandomColor());

		channel.sendMessage(eb.build()).queue();
	}
}
