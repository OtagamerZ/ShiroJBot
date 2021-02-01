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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardMarketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.MerchantStats;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "licenca",
		aliases = {"license", "merchantlicense", "licencadecomercio"},
		category = Category.INFO
)
public class MerchantLicenseCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		MerchantStats ms = CardMarketDAO.getMerchantStats(author.getId());
		MerchantStats avg = CardMarketDAO.getAverageMerchantStats();

		if (ms == null || avg == null) {
			channel.sendMessage("❌ | Você não possui dados de vendas suficientes neste mês.").queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Progresso para licença de " + author.getName())
				.setDescription("Possuir a licença de comércio reduz a taxa sobre vendas pela metade.")
				.addField("Obtida?", Helper.isTrustedMerchant(author.getId()) ? "SIM!!" : "Não", false)
				.addField("Cartas vendidas/necessário", (long) ms.getSold() + "/" + Math.round(avg.getSold() * 2), true)
				.addField("Compradores únicos/necessário", (int) ms.getUniqueBuyers() + "/" + Math.round(avg.getUniqueBuyers()), true)
				.setFooter("A licença ficará ativa enquanto você cumprir os requisitos, resetando todo mês.");

		channel.sendMessage(eb.build()).queue();
	}
}
