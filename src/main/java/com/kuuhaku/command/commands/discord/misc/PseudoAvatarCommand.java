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
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

import javax.imageio.ImageIO;
import java.io.IOException;

@Command(
		name = "pseudoavatar",
		aliases = {"pavatar"},
		usage = "req_link",
		category = Category.MISC
)
public class PseudoAvatarCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		com.kuuhaku.model.persistent.Member mb = MemberDAO.getMemberById(author.getId() + guild.getId());
		if (args.length == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-image")).queue();
			return;
		} else if (Helper.equalsAny(args[0], "reset", "limpar")) {
			mb.setPseudoAvatar("");
			MemberDAO.updateMemberConfigs(mb);
			channel.sendMessage("✅ | Pseudo-avatar limpo com sucesso!").queue();
			return;
		}

		try {
			ImageIO.read(Helper.getImage(args[0]));

			mb.setPseudoAvatar(args[0]);
			MemberDAO.updateMemberConfigs(mb);
			channel.sendMessage("✅ | Pseudo-avatar definido com sucesso!").queue();
		} catch (ClassCastException | IOException | NullPointerException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-image")).queue();
		}
	}
}
