/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.MarketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Market;
import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

@Command(
		name = "definiricone",
		aliases = {"seticon", "icone", "icon"},
		usage = "req_id-emoji",
		category = Category.MISC
)
public class SetMarketIconCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa informar o ID da carta no mercado.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | O ID precisa ser um valor inteiro.").queue();
			return;
		}

		Market m = MarketDAO.getCard(Integer.parseInt(args[0]));
		if (m == null) {
			channel.sendMessage("❌ | ID inválido ou a carta já foi comprada por alguém.").queue();
			return;
		} else if (!m.getSeller().equals(author.getId())) {
			channel.sendMessage("❌ | Você só pode alterar ícones nos seus próprios anúncios.").queue();
			return;
		}

		if (args.length < 2) {
			m.setEmoji(null);
			MarketDAO.saveCard(m);

			channel.sendMessage("✅ | Ícone removido com sucesso.").queue();
		} else {
			String emj = args[1];
			if (!EmojiUtils.isEmoji(emj)) {
				channel.sendMessage("❌ | O segundo argumento deve ser um **emoji** (emotes customizados não são emojis).").queue();
				return;
			}

			m.setEmoji(emj);
			MarketDAO.saveCard(m);

			channel.sendMessage("✅ | Ícone definido com sucesso.").queue();
		}
	}
}
