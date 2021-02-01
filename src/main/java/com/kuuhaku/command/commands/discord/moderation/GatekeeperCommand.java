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
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.apache.commons.lang3.StringUtils;

@Command(
		name = "porteiro",
		aliases = {"gatekeeper", "gk"},
		usage = "req_id-role",
		category = Category.MODERATION
)
public class GatekeeperCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length < 2) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_gatekeeper-no-id")).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-message-id")).queue();
			return;
		} else if (message.getMentionedRoles().size() == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_gatekeeper-no-role")).queue();
            return;
        }

		try {
			Helper.addButton(args, message, channel, gc, "☑", true);

			channel.sendMessage("✅ | Porteiro adicionado com sucesso!").queue(s -> Helper.gatekeep(gc));
		} catch (IllegalArgumentException e) {
			channel.sendMessage("❌ | Erro em um dos argumentos: " + e).queue();
		} catch (ErrorResponseException e) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_role-chooser-invalid-channel")).queue();
        }
	}
}
