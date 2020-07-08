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

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

public class KickMemberCommand extends Command {

	public KickMemberCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public KickMemberCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public KickMemberCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public KickMemberCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-member-to-ban")).queue();
			return;
		} else if (message.getMentionedUsers().size() > 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_too-many-mentions")).queue();
			return;
		} else if (!member.hasPermission(Permission.KICK_MEMBERS)) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_REV-kick-you-do-not-have-permission")).queue();
			return;
		} else if (!Helper.hasRoleHigherThan(member, message.getMentionedMembers().get(0)) || !Helper.hasRoleHigherThan(guild.getSelfMember(), message.getMentionedMembers().get(0))) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_REV-kick-you-cant-kick-him-out")).queue();
			return;
		} else if (Main.getInfo().getDevelopers().contains(message.getMentionedUsers().get(0).getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_REV-kick-you-can-not-do-that")).queue();
			return;
		}

        try {
            if (args.length < 2) {
                guild.kick(message.getMentionedMembers().get(0)).queue();
                channel.sendMessage("Membro expulso com sucesso!").queue();
            } else {
                guild.kick(message.getMentionedMembers().get(0), String.join(" ", args).replace(args[0], "").trim()).queue();
				channel.sendMessage("Membro expulso com sucesso!\nMotivo: `" + String.join(" ", args).replace(args[0], "").trim() + "`").queue();
            }
        } catch (InsufficientPermissionException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_REV-kick-not-permissions")).queue();
        }
    }
}
