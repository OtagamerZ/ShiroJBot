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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GiftCodeDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.GiftCode;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "giftcode",
		usage = "req_type-amount-code",
		category = Category.MISC
)
public class GiftCodeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa informar o tipo da operação (gerar ou resgatar) e a quantidade/código.").queue();
			return;
		}

		switch (args[0]) {
			case "gerar", "generate" -> {
				if (!ShiroInfo.getDevelopers().contains(author.getId())) {
					channel.sendMessage("❌ | Você não possui permissão para gerar gift codes.").queue();
					return;
				}

				try {
					int i = Integer.parseInt(args[1]);
					GiftCodeDAO.generateGiftCodes(i);
					channel.sendMessage("✅ | Códigos gerados com sucesso.").queue();
				} catch (NumberFormatException e) {
					channel.sendMessage("❌ | Quantidade inválida.").queue();
				}
			}
			case "resgatar", "redeem" -> {
				if (args[1].length() != 32) {
					channel.sendMessage("❌ | Código inválido.").queue();
					return;
				}

				GiftCode gc = GiftCodeDAO.redeemGiftCode(author.getId(), args[1]);
				if (gc == null) {
					channel.sendMessage("❌ | Código já utilizado.").queue();
					return;
				}
				gc.useCode(author.getId());

				channel.sendMessage("✅ | Código para `" + gc.getDescription() + "` resgatado com sucesso.").queue();
			}
		}
	}
}
