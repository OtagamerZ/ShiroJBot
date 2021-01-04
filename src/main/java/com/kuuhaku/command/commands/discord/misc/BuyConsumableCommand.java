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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.Consumable;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.ConsumableShop;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Map;

public class BuyConsumableCommand extends Command {

	public BuyConsumableCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BuyConsumableCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BuyConsumableCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BuyConsumableCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length < 1) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();
			eb.setTitle(":test_tube: | Loja de itens");
			eb.setDescription("Esta loja possui vários artefatos que podem lhe dar uma vantagem rápida em certas coisas, a um preço justo claro!");
			for (Map.Entry<String, Consumable> entry : ConsumableShop.getAvailable().entrySet()) {
				String k = entry.getKey();
				Consumable v = entry.getValue();
				eb.addField("`" + k + "` | " + v.getName() + " (" + v.getPrice() + " créditos)", v.getDescription(), false);
			}
			eb.setFooter("Seus créditos: " + acc.getBalance(), "https://i.imgur.com/U0nPjLx.gif");

			channel.sendMessage(eb.build()).queue();
			return;
		}

		if (!ConsumableShop.getAvailable().containsKey(args[0])) {
			channel.sendMessage("❌ | Esse item não existe.").queue();
			return;
		} else if (acc.getTotalBalance() < ConsumableShop.getAvailable().get(args[0]).getPrice()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
			return;
		}

		acc.addBuff(args[0]);
		acc.consumeCredit(ConsumableShop.getAvailable().get(args[0]).getPrice(), this.getClass());
		AccountDAO.saveAccount(acc);
		channel.sendMessage("Item comprado com sucesso, use `" + prefix + "usar " + args[0] + "` para usá-lo.").queue();
	}
}