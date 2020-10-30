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
import com.kuuhaku.controller.postgresql.LobbyDAO;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.controller.postgresql.QuizDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.common.RelayBlockList;
import com.kuuhaku.model.persistent.Lobby;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.requests.RestAction;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MinuteEvent implements Job {
	public static JobDetail unblock;
	public static boolean restarting = false;

	@Override
	public void execute(JobExecutionContext context) {
		if (LocalDateTime.now().getHour() % 6 == 0) {
			QuizDAO.resetUserStates();

			if (LocalDateTime.now().getHour() % 12 == 0) {
				RelayBlockList.clearBlockedThumbs();
				RelayBlockList.refresh();

			/*GuildDAO.getAllGuilds().forEach(gc -> {
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
			});*/
			}
		}

		MemberDAO.getMutedMembers().forEach(m -> {
			Guild g = Main.getInfo().getGuildByID(m.getGuild());
			if (g == null) {
				MemberDAO.removeMutedMember(m);
			} else {
				try {
					if (!g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) return;
					Member mb = g.getMemberById(m.getUid());
					Role r = g.getRoleById(GuildDAO.getGuildById(g.getId()).getCargoMute());
					assert r != null;
					assert mb != null;

					if (mb.getRoles().stream().filter(rol -> !rol.isPublicRole()).anyMatch(rol -> !rol.getId().equals(r.getId())) && m.isMuted()) {
						g.modifyMemberRoles(mb, r).queue(null, Helper::doNothing);
					} else if (!m.isMuted()) {
						List<Role> roles = m.getRoles().toList().stream().map(rol -> g.getRoleById((String) rol)).filter(Objects::nonNull).collect(Collectors.toList());
						g.modifyMemberRoles(mb, roles).queue(null, Helper::doNothing);
						MemberDAO.removeMutedMember(m);
					}
				} catch (HierarchyException ignore) {
				} catch (IllegalArgumentException | NullPointerException e) {
					MemberDAO.removeMutedMember(m);
				}
			}
		});

		Guild g = Main.getInfo().getGuildByID(ShiroInfo.getLobbyServerID());
		List<Lobby> lobbies = LobbyDAO.getLobbies();
		List<Category> categories = g.getCategories();
		for (Category cat : categories) {
			if (lobbies.stream().noneMatch(l -> cat.getName().startsWith(String.valueOf(l.getId())))) {
				cat.getChannels().stream()
						.map(GuildChannel::delete)
						.forEach(RestAction::queue);
				cat.delete().queue();
			}
		}
		categories = g.getCategories();
		for (Lobby lb : lobbies) {
			boolean exists = categories.stream()
					.anyMatch(c -> c.getName().startsWith(String.valueOf(lb.getId())));

			if (!exists) {
				g.createCategory(lb.getId() + " | " + lb.getName()).queue(s -> {
					g.createTextChannel("Salão")
							.setTopic("Sala de jogos criada por " + Main.getInfo().getUserByID(lb.getOwner()).getName())
							.setParent(s)
							.queue();

					g.createTextChannel("Call")
							.setUserlimit(lb.getMaxPlayers())
							.setParent(s)
							.queue();
				});
			}

			if (!lb.getPlayers().contains(lb.getOwner()) && Main.getInfo().getPendingJoin().getIfPresent(lb.getOwner()) == null)
				LobbyDAO.deleteLobby(lb);
		}

		List<Member> members = g.getMembers();
		for (Member mb : members) {
			if (lobbies.stream().noneMatch(l -> l.getPlayers().contains(mb.getId()) || mb.getRoles().stream().anyMatch(r -> !r.isPublicRole())))
				g.kick(mb).queue();
		}
	}
}
