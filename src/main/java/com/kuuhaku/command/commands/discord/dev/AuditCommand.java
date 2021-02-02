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

package com.kuuhaku.command.commands.discord.dev;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.LogDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "audit",
		aliases = {"auditar"},
		usage = "req_type-id",
		category = Category.DEV
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class AuditCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage("❌ | É necessário informar o tipo de auditoria (`T` = transações e `C` = comandos) e o ID do usuário.").queue();
			return;
		} else if (!Helper.equalsAny(args[0], "T", "C")) {
			channel.sendMessage("❌ | O tipo de auditoria deve ser `T` = transações ou `C` = comandos.").queue();
			return;
		}

		User usr = Main.getInfo().getUserByID(args[1]);

		if (usr == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-id")).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Dados de auditoria de %s (%s)".formatted(
						usr.getName(),
						args[0].equalsIgnoreCase("T") ? "Transações" : "Comandos"
				));

		List<List<Object[]>> data = Helper.chunkify(LogDAO.auditUser(usr.getId(), args[0]), 20);
		List<Page> pages = new ArrayList<>();

		StringBuilder sb = new StringBuilder();
		for (List<Object[]> chunk : data) {
			sb.setLength(0);
			if (args[0].equalsIgnoreCase("T"))
				for (Object[] entry : chunk)
					sb.append("`%s`: %s créditos\n".formatted(StringUtils.abbreviate(String.valueOf(entry[0]), 60), Helper.separate(entry[1])));
			else
				for (Object[] entry : chunk)
					sb.append("`%s`: %s usos\n".formatted(StringUtils.abbreviate(String.valueOf(entry[0]), 60), entry[1]));

			eb.setDescription(sb.toString());
			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
