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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.VipItem;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

@Command(
		name = "vip",
		aliases = {"lojavip", "gemshop"},
		usage = "req_id",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class VipStoreCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length == 0) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle(":diamonds: | Loja VIP");
			eb.setDescription("""
					Gemas podem ser obtidas ao resgatar um acúmulo de 7 votos seguidos com o comando `%sresgatar`. Para utilizar as gemas basta usar `%svip ID`!
					     
					Muito obrigada por me apoiar!
					""".formatted(prefix, prefix));
			for (VipItem v : VipItem.values()) eb.addField(v.getField());
			eb.setColor(Color.red);
			eb.setFooter("Suas gemas: " + acc.getGems(), "https://bgasparotto.com/wp-content/uploads/2016/03/ruby-logo.png");

			channel.sendMessage(eb.build()).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-id-value")).queue();
			return;
		}

		VipItem vi = VipItem.getById(Integer.parseInt(args[0]));
		if (vi == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-id")).queue();
			return;
		} else if (acc.getGems() < vi.getGems()) {
			channel.sendMessage("❌ | Você não possui gemas suficientes.").queue();
			return;
		}

		vi.getAction().accept(channel, acc, args);
	}
}
