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
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

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
							**Tier 1** (2500 créditos): `+30% XP ganho` (15 dias)
							**Tier 2** (5000 créditos): `+60% XP ganho` (11 dias)
							**Tier 3** (7500 créditos): `+90% XP ganho` (7 dias)
							"""
					, false);
			eb.addBlankField(false);
			eb.addField("Melhoria de cartas (`" + prefix + "up carta TIER`)", """
							**Tier 1** (2000 créditos): `+20% chance de aparecer cartas` (15 dias)
							**Tier 2** (4000 créditos): `+30% chance de aparecer cartas` (11 dias)
							**Tier 3** (6000 créditos): `+40% chance de aparecer cartas` (7 dias)
							**:warning: Tier Ultimate** (60000 créditos): `Uma completa loucura, por 1 minuto TODAS as mensagens farão aparecer cartas`
							"""
					, false);
			eb.addBlankField(false);
			eb.addField("Melhoria de drops (`" + prefix + "up drop TIER`)", """
							**Tier 1** (1400 créditos): `+20% chance de aparecer drops` (15 dias)
							**Tier 2** (2800 créditos): `+30% chance de aparecer drops` (11 dias)
							**Tier 3** (4200 créditos): `+40% chance de aparecer drops` (7 dias)
							**:warning: Tier Ultimate** (42000 créditos): `Uma completa loucura, por 1 minuto TODAS as mensagens farão aparecer drops`
							"""
					, false);
			eb.addBlankField(false);
			eb.addField("Melhoria de cartas cromadas (`" + prefix + "up cromada TIER`)", """
							**Tier 1** (4000 créditos): `+25% chance de aparecer cartas cromadas` (15 dias)
							**Tier 2** (8000 créditos): `+50% chance de aparecer cartas cromadas` (11 dias)
							**Tier 3** (12000 créditos): `+75% chance de aparecer cartas cromadas` (7 dias)
							**:warning: Tier Ultimate** (120000 créditos): `Uma completa loucura, por 1 minuto TODAS as cartas que aparecerem serão cromadas`
							"""
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
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
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
		if (tier != 4) channel.sendMessage("✅ | Melhoria aplicada com sucesso! (" + sb.getTime() + " dias).").queue();
		else channel.sendMessage("✅ | Melhoria aplicada com sucesso! (" + sb.getTime() + " minuto). CORRA!!!").queue();
	}
}
