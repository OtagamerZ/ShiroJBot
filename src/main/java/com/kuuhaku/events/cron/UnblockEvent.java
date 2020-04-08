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

package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.controller.postgresql.QuizDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.common.RelayBlockList;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UnblockEvent implements Job {
	public static JobDetail unblock;

	@Override
	public void execute(JobExecutionContext context) {
		if (LocalDateTime.now().getHour() % 12 == 0) {
			RelayBlockList.clearBlockedThumbs();
			RelayBlockList.refresh();

			GuildDAO.getAllGuilds().forEach(gc -> {
				if (gc.getCargoVip() != null && !gc.getCargoVip().isEmpty()) {
					Guild g = Main.getInfo().getGuildByID(gc.getGuildID());
					if (!g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) return;
					Role r = g.getRoleById(GuildDAO.getGuildById(g.getId()).getCargoVip());
					assert r != null;
					g.retrieveInvites().complete().stream()
							.filter(inv -> inv.getInviter() != null && inv.getInviter() != Objects.requireNonNull(g.getOwner()).getUser() && !inv.getInviter().isFake() && !inv.getInviter().isBot())
							.map(inv -> {
								Member m = g.getMember(inv.getInviter());
								assert m != null;
								if (inv.getUses() / TimeUnit.DAYS.convert(m.getTimeJoined().toEpochSecond(), TimeUnit.SECONDS) > 1)
									return g.addRoleToMember(m, r);
								else return g.removeRoleFromMember(m, r);
							}).collect(Collectors.toList())
							.forEach(rst -> {
								try {
									rst.complete();
								} catch (NullPointerException ignore) {
								}
							});
				}
			});
		} else if (LocalDateTime.now().getHour() % 6 == 0) {
			QuizDAO.resetUserStates();
		}

		MemberDAO.getMutedMembers().forEach(m -> {
			Guild g = Main.getInfo().getGuildByID(m.getGuild());
			if (!g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) return;
			Member mb = g.getMemberById(m.getUid());
			Role r = g.getRoleById(GuildDAO.getGuildById(g.getId()).getCargoWarn());
			assert r != null;
			assert mb != null;
			if (mb.getRoles().stream().filter(rol -> !rol.isPublicRole()).anyMatch(rol -> !rol.getId().equals(r.getId())) && m.isMuted()) {
				g.modifyMemberRoles(mb, r).complete();
			} else if (!m.isMuted()) {
				List<Role> roles = m.getRoles().toList().stream().map(rol -> g.getRoleById((String) rol)).collect(Collectors.toList());
				g.modifyMemberRoles(mb, roles).complete();
				MemberDAO.removeMutedMember(m);
			}
		});
	}
}
