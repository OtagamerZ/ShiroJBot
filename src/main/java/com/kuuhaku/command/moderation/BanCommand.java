/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Command(
		name = "ban",
		category = Category.MODERATION
)
@Signature({
		"<reason:text:r> <users:user:r>",
		"<reason:text:r> <ids:text:r>"
})
@Requires({Permission.BAN_MEMBERS})
public class BanCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		List<Member> members;
		if (args.containsKey("users")) {
			members = event.message().getMentionedMembers(event.guild());
		} else {
			members = Arrays.stream(args.getString("ids").split(" +"))
					.filter(StringUtils::isNumeric)
					.map(event.guild()::getMemberById)
					.filter(Objects::nonNull)
					.toList();
		}

		for (Member mb : members) {
			if (mb.equals(event.member())) {
				event.channel().sendMessage(locale.get("error/cant_ban_yourself")).queue();
				return;
			} else if (!event.member().canInteract(mb)) {
				event.channel().sendMessage(locale.get("error/cant_ban_user")).queue();
				return;
			}
		}

		Utils.confirm(locale.get("question/ban",
						members.size() == 1 ? locale.get("str/that_m") : locale.get("str/those_m"),
						members.size() == 1 ? locale.get("str/user") : locale.get("str/users")
				), event.channel(), wrapper ->
						RestAction.allOf(members.stream().map(m -> m.ban(7, args.getString("reason"))).toList())
								.flatMap(s -> event.channel().sendMessage(locale.get("success/ban",
												s.size(),
												s.size() == 1 ? "" : "s",
												args.get("reason")
										))
								).queue(),
				event.user()
		);
	}
}
