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

package com.kuuhaku.command.commands.discord.support;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.RatingDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.SupportItem;
import com.kuuhaku.model.persistent.SupportRating;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

@Command(
		name = "lojasup",
		aliases = {"lojadossuportes", "supshop"},
		usage = "req_id",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class SupportStoreCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		SupportRating sr = RatingDAO.getRating(author.getId());

		if (args.length == 0) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle(":nazar_amulet: | Loja dos Suportes");
			eb.setDescription("""
					Tokens de suporte são obtidos quando um usuário te agradece usando o comando `%sobrigado`. Para utilizar os tokens basta usar `%slojasup ID`!
					     
					Sua ajuda é essencial!
					""".formatted(prefix, prefix));
			for (SupportItem s : SupportItem.values()) eb.addField(s.getField());
			eb.setColor(Color.decode("#0ec86b"));
			eb.setFooter("Seus tokens: " + sr.getThanksTokens(), "https://cdn-0.emojis.wiki/emoji-pics/microsoft/nazar-amulet-microsoft.png");

			channel.sendMessage(eb.build()).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-id-value")).queue();
			return;
		}

		SupportItem si = SupportItem.getById(Integer.parseInt(args[0]));
		if (si == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-id")).queue();
			return;
		} else if (sr.getThanksTokens() < si.getTokens()) {
			channel.sendMessage("❌ | Você não possui tokens suficientes.").queue();
			return;
		}

		si.getAction().accept(channel, sr, args);
	}
}
