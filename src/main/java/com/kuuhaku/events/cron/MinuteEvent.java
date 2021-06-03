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

package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.BotStatsDAO;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.handlers.api.websocket.EncoderClient;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MinuteEvent implements Job {
	public static JobDetail minute;

	@Override
	public void execute(JobExecutionContext context) {
		BotStatsDAO.register();

		if (Main.getInfo().getEncoderClient().isClosed()) {
			try {
				if (!Main.getInfo().getEncoderClient().reconnectBlocking()) {
					Main.getInfo().setEncoderClient(new EncoderClient(ShiroInfo.SOCKET_ROOT + "/encoder"));
				}
			} catch (InterruptedException | URISyntaxException ignore) {
			}
		}

		for (MutedMember m : MemberDAO.getMutedMembers()) {
			Guild g = Main.getInfo().getGuildByID(m.getGuild());
			if (g == null) {
				MemberDAO.removeMutedMember(m);
			} else {
				try {
					if (!g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) continue;
					Member mb = g.getMemberById(m.getUid());
					Role r = GuildDAO.getGuildById(g.getId()).getMuteRole();
					assert r != null;
					assert mb != null;

					if (mb.getRoles().stream().filter(rol -> !rol.isPublicRole()).anyMatch(rol -> !rol.getId().equals(r.getId())) && m.isMuted()) {
						g.modifyMemberRoles(mb, r).queue(null, Helper::doNothing);
					} else if (!m.isMuted()) {
						List<Role> roles = m.getRoles().toList().stream()
								.map(rol -> g.getRoleById(rol.getAsString()))
								.filter(Objects::nonNull)
								.collect(Collectors.toList());
						g.modifyMemberRoles(mb, roles).queue(null, Helper::doNothing);
						MemberDAO.removeMutedMember(m);
					}
				} catch (HierarchyException ignore) {
				} catch (IllegalArgumentException | NullPointerException e) {
					MemberDAO.removeMutedMember(m);
				}
			}
		}
	}
}
