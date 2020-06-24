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

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
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
		if (args.length < 2) {
			EmbedBuilder eb = new EmbedBuilder();

			eb.setTitle(":level_slider: | Melhorias de servidor");
			eb.setDescription("Melhorias são aplicadas a todos os membros do servidor por um certo período, use-as para oferecer vantagens aos seus membros.");
			eb.addField("Melhoria de XP (" + prefix + "up xp)",
					"**Tier 1** (1500 créditos): `+50% XP ganho`\n" +
							"**Tier 2** (4000 créditos): `+100% XP ganho`\n" +
							"**Tier 3** (10000 créditos): `+200% XP ganho`",
					false);
			eb.addField("Melhoria de cartas (" + prefix + "up carta)",
					"**Tier 1** (1000 créditos): `+20% chance de aparecer cartas`\n" +
							"**Tier 2** (3000 créditos): `+30% chance de aparecer cartas`\n" +
							"**Tier 3** (5000 créditos): `+40% chance de aparecer cartas`",
					false);
			eb.addField("Melhoria de drops (" + prefix + "up drop)",
					"**Tier 1** (1250 créditos): `+20% chance de aparecer drops`\n" +
							"**Tier 2** (2750 créditos): `+30% chance de aparecer drops`\n" +
							"**Tier 3** (6000 créditos): `+40% chance de aparecer drops`",
					false);
			eb.addField("Melhoria de cartas cromadas (" + prefix + "up cromo)",
					"**Tier 1** (5000 créditos): `+20% chance de aparecer cartas cromadas`\n" +
							"**Tier 2** (8000 créditos): `+50% chance de aparecer cartas cromadas`\n" +
							"**Tier 3** (12000 créditos): `+100% chance de aparecer cartas cromadas`",
					false);

			channel.sendMessage(eb.build()).queue();
			return;
		} else if (!Helper.containsAny(args[0], "xp", "carta", "drop", "cromo")) {
			channel.sendMessage(":x: | O tipo da melhoria deve ser um dos seguintes tipos: `xp`, `carta`, `drop` ou `cromo`.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[1])) {
			channel.sendMessage(":x: | O tier da melhoria deve ser um valor entre 1 e 3.").queue();
			return;
		}

		int tier = Integer.parseInt(args[1]);
		if (tier < 1 || tier > 3) {
			channel.sendMessage(":x: | O tier da melhoria deve ser um valor entre 1 e 3.").queue();
			return;
		}

		ServerBuff sb = null;
		switch (args[0].toUpperCase()) {
			case "XP":
				switch (tier) {
					case 1:
						sb = new ServerBuff(tier, ServerBuff.XP_TIER_1);
					case 2:
						sb = new ServerBuff(tier, ServerBuff.XP_TIER_2);
					case 3:
						sb = new ServerBuff(tier, ServerBuff.XP_TIER_3);
				}
			case "CARTA":
				switch (tier) {
					case 1:
						sb = new ServerBuff(tier, ServerBuff.CARD_TIER_1);
					case 2:
						sb = new ServerBuff(tier, ServerBuff.CARD_TIER_2);
					case 3:
						sb = new ServerBuff(tier, ServerBuff.CARD_TIER_3);
				}
			case "DROP":
				switch (tier) {
					case 1:
						sb = new ServerBuff(tier, ServerBuff.DROP_TIER_1);
					case 2:
						sb = new ServerBuff(tier, ServerBuff.DROP_TIER_2);
					case 3:
						sb = new ServerBuff(tier, ServerBuff.DROP_TIER_3);
				}
			case "CROMADA":
				switch (tier) {
					case 1:
						sb = new ServerBuff(tier, ServerBuff.FOIL_TIER_1);
					case 2:
						sb = new ServerBuff(tier, ServerBuff.FOIL_TIER_2);
					case 3:
						sb = new ServerBuff(tier, ServerBuff.FOIL_TIER_3);
				}
		}

		assert sb != null;
		Account acc = AccountDAO.getAccount(author.getId());
		if (acc.getBalance() < sb.getPrice()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		if (!gc.addBuff(sb)) {
			channel.sendMessage(":x: | Este servidor já possui uma melhoria dessa categoria.").queue();
			return;
		}

		GuildDAO.updateGuildSettings(gc);
		channel.sendMessage(":x: | melhoria aplicada com sucesso! (" + sb.getTime() + " dias).").queue();
	}
}
