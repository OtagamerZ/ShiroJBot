/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.moderation;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.SigPattern;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.model.persistent.user.Profile;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Command(
		name = "warn",
		category = Category.MODERATION
)
@Signature(
		patterns = {
				@SigPattern(id = "users", value = "(<@!?(\\d+)>\\s*)+"),
				@SigPattern(id = "ids", value = "(\\d+\\s*)+")
		},
		value = {
				"<users:custom:r>[users] <reason:text:r>",
				"<ids:custom:r>[ids] <reason:text:r>"
		}
)
@Requires({
		Permission.MODERATE_MEMBERS,
		Permission.KICK_MEMBERS,
		Permission.BAN_MEMBERS
})
public class WarnCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		List<Member> members;
		if (args.has("users")) {
			members = event.message().getMentions().getMembers();
		} else {
			members = Arrays.stream(args.getString("ids").split(" +"))
					.filter(StringUtils::isNumeric)
					.map(event.guild()::getMemberById)
					.filter(Objects::nonNull)
					.toList();
		}

		Member self = event.guild().getSelfMember();
		for (Member mb : members) {
			if (mb.equals(event.member())) {
				event.channel().sendMessage(locale.get("error/cant_warn_yourself")).queue();
				return;
			} else if (!event.member().canInteract(mb)) {
				event.channel().sendMessage(locale.get("error/cant_warn_user")).queue();
				return;
			} else if (!self.canInteract(mb)) {
				event.channel().sendMessage(locale.get("error/higher_hierarchy")).queue();
				return;
			}
		}

		for (Member mb : members) {
			Profile p = DAO.find(Profile.class, new ProfileId(mb.getId(), event.guild().getId()));
			p.warn(event.user(), args.getString("reason"));
		}

		event.channel().sendMessage(locale.get("success/warn")).queue();
	}
}
