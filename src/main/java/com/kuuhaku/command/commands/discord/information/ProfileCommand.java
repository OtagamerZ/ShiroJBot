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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

public class ProfileCommand extends Command {

	public ProfileCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ProfileCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ProfileCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ProfileCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		com.kuuhaku.model.persistent.Member mb = MemberDAO.getMemberById(author.getId() + guild.getId());
		channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_generating-profile")).queue(m -> {
			try {
				if (acc.hasAnimatedBg() && Helper.getFileType(acc.getBg()).contains("gif")) {
					File pf = Profile.applyAnimatedBackground(acc, Profile.makeProfile(member, guild));
					channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_profile"), author.getAsMention())).addFile(pf, "perfil.gif").queue(s -> m.delete().queue());
				} else
					channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_profile"), author.getAsMention())).addFile(Helper.getBytes(Profile.makeProfile(member, guild), "png"), "perfil.png").queue(s -> m.delete().queue());
			} catch (IOException e) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_profile-generation-error")).queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			} catch (InsufficientPermissionException e) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-attach-files-permission")).queue();
			}
		});
	}
}
