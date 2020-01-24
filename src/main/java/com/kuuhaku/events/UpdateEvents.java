/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.events;

import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.events.guild.member.update.GenericGuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.update.GenericGuildUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateEvents extends ListenerAdapter {
	@Override
	public void onGenericGuildUpdate(@Nonnull GenericGuildUpdateEvent event) {
		List<String> mbs = MemberDAO.getRegisteredUsers().stream().map(Object::toString).collect(Collectors.toList());
		List<String> ids = event.getGuild().getMembers().stream().filter(m -> mbs.contains(m.getId())).map(net.dv8tion.jda.api.entities.Member::getId).collect(Collectors.toList());

		Helper.notifyGuildUpdate(ids, event.getGuild().getId());
	}

	@Override
	public void onGenericGuildMemberUpdate(@Nonnull GenericGuildMemberUpdateEvent event) {
		Helper.notifyMemberUpdate(event.getUser().getId(), event.getUser().getId());
	}
}
