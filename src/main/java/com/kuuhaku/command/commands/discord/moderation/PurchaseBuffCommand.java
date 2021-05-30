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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.GuildBuffDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.BuffType;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.guild.GuildBuff;
import com.kuuhaku.model.persistent.guild.ServerBuff;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

@Command(
		name = "melhorar",
		aliases = {"upgrade", "up"},
		usage = "req_type-tier",
		category = Category.MODERATION
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class PurchaseBuffCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length < 2) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setTitle(":level_slider: | Melhorias de servidor");
			eb.setDescription("Melhorias são aplicadas a todos os membros do servidor por um certo período, use-as para oferecer vantagens aos seus membros.");
			eb.addField("Melhoria de XP (`" + prefix + "up xp TIER`)", """
							**Tier 1** (%s créditos): `+30%% XP ganho` (15 dias)
							**Tier 2** (%s créditos): `+60%% XP ganho` (11 dias)
							**Tier 3** (%s créditos): `+90%% XP ganho` (7 dias)
							"""
							.formatted(
									Helper.separate(new ServerBuff(BuffType.XP, 1).getPrice()),
									Helper.separate(new ServerBuff(BuffType.XP, 2).getPrice()),
									Helper.separate(new ServerBuff(BuffType.XP, 3).getPrice())
							)
					, false);
			eb.addBlankField(false);
			eb.addField("Melhoria de cartas (`" + prefix + "up carta TIER`)", """
							**Tier 1** (%s créditos): `+20%% chance de aparecer cartas` (15 dias)
							**Tier 2** (%s créditos): `+30%% chance de aparecer cartas` (11 dias)
							**Tier 3** (%s créditos): `+40%% chance de aparecer cartas` (7 dias)
							**:warning: Tier Ultimate** (%s créditos): `Uma completa loucura, por 1 minuto TODAS as mensagens farão aparecer cartas`
							"""
							.formatted(
									Helper.separate(new ServerBuff(BuffType.CARD, 1).getPrice()),
									Helper.separate(new ServerBuff(BuffType.CARD, 2).getPrice()),
									Helper.separate(new ServerBuff(BuffType.CARD, 3).getPrice()),
									Helper.separate(new ServerBuff(BuffType.CARD, 4).getPrice())
							)
					, false);
			eb.addBlankField(false);
			eb.addField("Melhoria de drops (`" + prefix + "up drop TIER`)", """
							**Tier 1** (%s créditos): `+20%% chance de aparecer drops` (15 dias)
							**Tier 2** (%s créditos): `+30%% chance de aparecer drops` (11 dias)
							**Tier 3** (%s créditos): `+40%% chance de aparecer drops` (7 dias)
							**:warning: Tier Ultimate** (%s créditos): `Uma completa loucura, por 1 minuto TODAS as mensagens farão aparecer drops`
							"""
							.formatted(
									Helper.separate(new ServerBuff(BuffType.DROP, 1).getPrice()),
									Helper.separate(new ServerBuff(BuffType.DROP, 2).getPrice()),
									Helper.separate(new ServerBuff(BuffType.DROP, 3).getPrice()),
									Helper.separate(new ServerBuff(BuffType.DROP, 4).getPrice())
							)
					, false);
			eb.addBlankField(false);
			eb.addField("Melhoria de cartas cromadas (`" + prefix + "up cromada TIER`)", """
							**Tier 1** (%s créditos): `+25%% chance de aparecer cartas cromadas` (15 dias)
							**Tier 2** (%s créditos): `+50%% chance de aparecer cartas cromadas` (11 dias)
							**Tier 3** (%s créditos): `+75%% chance de aparecer cartas cromadas` (7 dias)
							**:warning: Tier Ultimate** (%s créditos): `Uma completa loucura, por 1 minuto TODAS as cartas que aparecerem serão cromadas`
							"""
							.formatted(
									Helper.separate(new ServerBuff(BuffType.FOIL, 1).getPrice()),
									Helper.separate(new ServerBuff(BuffType.FOIL, 2).getPrice()),
									Helper.separate(new ServerBuff(BuffType.FOIL, 3).getPrice()),
									Helper.separate(new ServerBuff(BuffType.FOIL, 4).getPrice())
							)
					, false);
			eb.setFooter("Seus créditos: " + acc.getBalance(), "https://i.imgur.com/U0nPjLx.gif");

			channel.sendMessage(eb.build()).queue();
			return;
		} else if (!Helper.equalsAny(args[0], "xp", "carta", "drop", "cromada")) {
			channel.sendMessage("❌ | O tipo da melhoria deve ser um dos seguintes tipos: `xp`, `carta`, `drop` ou `cromada`.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[1]) && !args[1].equalsIgnoreCase("ultimate")) {
			channel.sendMessage("❌ | O tier da melhoria deve ser um valor entre 1 e 3 ou `ultimate`.").queue();
			return;
		}

		int tier = args[1].equalsIgnoreCase("ultimate") ? 4 : Integer.parseInt(args[1]);
		if (!Helper.between(tier, 1, 5)) {
			channel.sendMessage("❌ | O tier da melhoria deve ser um valor entre 1 e 3 ou `ultimate`.").queue();
			return;
		}

		ServerBuff sb = new ServerBuff(BuffType.of(args[0]), tier);

		if (acc.getTotalBalance() < sb.getPrice()) {
			channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
			return;
		}

		acc.consumeCredit(sb.getPrice(), this.getClass());
		GuildBuff gb = GuildBuffDAO.getBuffs(guild.getId());
		if (!gb.addBuff(sb)) {
			channel.sendMessage("❌ | Este servidor já possui uma melhoria de tier superior nessa categoria.").queue();
			return;
		}

		GuildBuffDAO.saveBuffs(gb);
		AccountDAO.saveAccount(acc);
		if (tier != 4)
			channel.sendMessage("✅ | Melhoria aplicada com sucesso! (" + TimeUnit.DAYS.convert(sb.getTime(), TimeUnit.MILLISECONDS) + " dias).").queue();
		else
			channel.sendMessage("✅ | Melhoria aplicada com sucesso! (" + TimeUnit.MINUTES.convert(sb.getTime(), TimeUnit.MILLISECONDS) + " minuto). CORRA!!!").queue();
	}
}
