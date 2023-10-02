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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.io.IOException;

@Command(
		name = "perfil",
		aliases = {"xp", "profile", "pf"},
		usage = "req_mention-opt",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EXT_EMOJI})
public class ProfileCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Member mb;
		if (message.getMentionedMembers().isEmpty()) mb = member;
		else mb = message.getMentionedMembers().get(0);

		Account acc = AccountDAO.getAccount(mb.getId());

		channel.sendMessage(I18n.getString("str_generating-profile")).queue(m -> {
			try {
				channel.sendMessage(I18n.getString("str_profile", Helper.unmention(mb.getUser().getName())))
						.addFile(Helper.writeAndGet(Profile.makeProfile(mb, mb.getGuild()), "perfil", "png"))
						.flatMap(s -> m.delete())
						.queue(null, t -> m.editMessage(I18n.getString("err_profile-too-big")).queue());
			} catch (IOException | NullPointerException e) {
				m.editMessage(I18n.getString("err_profile-generation-error")).queue();
			} catch (InsufficientPermissionException e) {
				m.editMessage(I18n.getString("err_no-attach-files-permission")).queue();
			}
		});
	}
}
