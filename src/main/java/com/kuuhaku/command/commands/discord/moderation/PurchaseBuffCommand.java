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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.GuildBuffDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.GuildBuff;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ServerBuff;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

public class PurchaseBuffCommand extends Command {

	public PurchaseBuffCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PurchaseBuffCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PurchaseBuffCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PurchaseBuffCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length < 2) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setTitle(":level_slider: | Melhorias de servidor");
			eb.setDescription("Melhorias são aplicadas a todos os membros do servidor por um certo período, use-as para oferecer vantagens aos seus membros.");
			eb.addField("Melhoria de XP (`" + prefix + "up xp TIER`)", """
							**Tier 1** (1500 créditos): `+50% XP ganho` (15 dias)
							**Tier 2** (4000 créditos): `+75% XP ganho` (10 dias)
							**Tier 3** (10000 créditos): `+100% XP ganho` (5 dias)
							"""
					, false);
			eb.addBlankField(false);
			eb.addField("Melhoria de cartas (`" + prefix + "up carta TIER`)", """
							**Tier 1** (1000 créditos): `+20% chance de aparecer cartas` (15 dias)
							**Tier 2** (3000 créditos): `+30% chance de aparecer cartas` (10 dias)
							**Tier 3** (5000 créditos): `+40% chance de aparecer cartas` (5 dias)
							**:warning: Tier Ultimate** (50000 créditos): `Uma completa loucura, por 1 minuto TODAS as mensagens farão aparecer cartas`
							"""
					, false);
			eb.addBlankField(false);
			eb.addField("Melhoria de drops (`" + prefix + "up drop TIER`)", """
							**Tier 1** (1250 créditos): `+20% chance de aparecer drops` (15 dias)
							**Tier 2** (3500 créditos): `+30% chance de aparecer drops` (10 dias)
							**Tier 3** (6000 créditos): `+40% chance de aparecer drops` (5 dias)
							**:warning: Tier Ultimate** (60000 créditos): `Uma completa loucura, por 1 minuto TODAS as mensagens farão aparecer drops`
							"""
					, false);
			eb.addBlankField(false);
			eb.addField("Melhoria de cartas cromadas (`" + prefix + "up cromada TIER`)", """
							**Tier 1** (5000 créditos): `+20% chance de aparecer cartas cromadas` (15 dias)
							**Tier 2** (8000 créditos): `+50% chance de aparecer cartas cromadas` (10 dias)
							**Tier 3** (12000 créditos): `+100% chance de aparecer cartas cromadas` (5 dias)
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
		if (tier < 1 || tier > 4) {
			channel.sendMessage("❌ | O tier da melhoria deve ser um valor entre 1 e 3 ou `ultimate`.").queue();
			return;
		}

		ServerBuff sb = null;
		switch (args[0].toUpperCase()) {
			case "XP" -> sb = switch (tier) {
				case 1 -> new ServerBuff(tier, ServerBuff.XP_TIER_1);
				case 2 -> new ServerBuff(tier, ServerBuff.XP_TIER_2);
				case 3 -> new ServerBuff(tier, ServerBuff.XP_TIER_3);
				default -> null;
			};
			case "CARTA" -> sb = switch (tier) {
				case 1 -> new ServerBuff(tier, ServerBuff.CARD_TIER_1);
				case 2 -> new ServerBuff(tier, ServerBuff.CARD_TIER_2);
				case 3 -> new ServerBuff(tier, ServerBuff.CARD_TIER_3);
				case 4 -> new ServerBuff(tier, ServerBuff.CARD_TIER_U);
				default -> sb;
			};
			case "DROP" -> sb = switch (tier) {
				case 1 -> new ServerBuff(tier, ServerBuff.DROP_TIER_1);
				case 2 -> new ServerBuff(tier, ServerBuff.DROP_TIER_2);
				case 3 -> new ServerBuff(tier, ServerBuff.DROP_TIER_3);
				case 4 -> new ServerBuff(tier, ServerBuff.DROP_TIER_U);
				default -> sb;
			};
			case "CROMADA" -> sb = switch (tier) {
				case 1 -> new ServerBuff(tier, ServerBuff.FOIL_TIER_1);
				case 2 -> new ServerBuff(tier, ServerBuff.FOIL_TIER_2);
				case 3 -> new ServerBuff(tier, ServerBuff.FOIL_TIER_3);
				case 4 -> new ServerBuff(tier, ServerBuff.FOIL_TIER_U);
				default -> sb;
			};
		}

		if (sb == null) {
			channel.sendMessage("❌ | Melhoria inválida, use `" + prefix + "up` para ver a lista de melhorias.").queue();
			return;
		}

		if (acc.getTotalBalance() < sb.getPrice()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
			return;
		}

		acc.consumeCredit(sb.getPrice(), this.getClass());
		GuildBuff gb = GuildBuffDAO.getBuffs(guild.getId());
		if (!gb.addBuff(sb)) {
			channel.sendMessage("❌ | Este servidor já possui uma melhoria dessa categoria.").queue();
			return;
		}

		GuildBuffDAO.saveBuffs(gb);
		AccountDAO.saveAccount(acc);
		if (tier != 4) channel.sendMessage("✅ | Melhoria aplicada com sucesso! (" + sb.getTime() + " dias).").queue();
		else channel.sendMessage("✅ | Melhoria aplicada com sucesso! (" + sb.getTime() + " minuto). CORRA!!!").queue();
	}
}
