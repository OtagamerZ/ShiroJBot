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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.utils.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TenthMinuteEvent implements Job {
	public static JobDetail check;

	@Override
	public void execute(JobExecutionContext context) {
		Main.getJibril().getGuilds().forEach(TenthMinuteEvent::notif);
		AccountDAO.getNotifiableAccounts().forEach(Account::notifyVote);

		for (Guild g : Main.getInfo().getAPI().getGuilds()) {
			GuildMusicManager gmm = Music.getGuildAudioPlayer(g, null);
			if (g.getAudioManager().isConnected() && (Objects.requireNonNull(g.getAudioManager().getConnectedChannel()).getMembers().size() < 1)) {
				g.getAudioManager().closeAudioConnection();
				gmm.scheduler.channel.sendMessage("Me deixaram sozinha no chat de voz, então eu saí também!").queue();
				gmm.scheduler.clear();
				gmm.player.destroy();
			}
		}

		List<ExceedMember> ems = ExceedDAO.getExceedMembers();
		List<Member> mbs = Main.getInfo().getGuildByID(ShiroInfo.getSupportServerID()).getMembers()
				.stream()
				.filter(mb -> ems.stream().anyMatch(em -> em.getId().equals(mb.getId())))
				.collect(Collectors.toList());

		Guild support = Main.getInfo().getGuildByID(ShiroInfo.getSupportServerID());

		assert support != null;
		mbs.forEach(mb -> {
			List<Role> roles = new ArrayList<>(mb.getRoles());
			ExceedEnum ex = ExceedEnum.getByName(ExceedDAO.getExceed(mb.getId()));
			Role exRole = support.getRoleById(ExceedRole.getByExceed(ex).getId());

			if (roles.stream().anyMatch(r -> {
				ExceedRole role = ExceedRole.getById(r.getId());
				return role != null && role.getExceed() != ex;
			})) {
				roles.removeIf(r -> {
					ExceedRole role = ExceedRole.getById(r.getId());
					return role != null && role.getExceed() != ex;
				});

				support.modifyMemberRoles(mb, roles).queue();
			}

			assert exRole != null;
			if (roles.stream().noneMatch(r -> r.getId().equals(exRole.getId()))) {
				support.addRoleToMember(mb, exRole).queue();
			}
		});
	}

	private static void notif(Guild g) {
		try {
			if (!TagDAO.getTagById(g.getOwnerId()).isBeta() && !ShiroInfo.getDevelopers().contains(g.getOwnerId())) {
				Main.getJibril().retrieveUserById(ShiroInfo.getNiiChan()).queue(o -> {
							Helper.logger(TenthMinuteEvent.class).info("Saí do servidor " + g.getName() + " por " + Objects.requireNonNull(g.getOwner()).getUser().getAsTag() + " não estar na lista de parceiros.");
							g.leave().queue();
						}
						, f -> {
							Helper.logger(TenthMinuteEvent.class).info("Saí do servidor " + g.getName() + " por DESCONHECIDO não estar na lista de parceiros.");
							g.leave().queue();
						}
				);
			}
		} catch (NoResultException e) {
			Main.getJibril().retrieveUserById(ShiroInfo.getNiiChan()).queue(o -> {
						Helper.logger(TenthMinuteEvent.class).info("Saí do servidor " + g.getName() + " por " + o.getUser().getAsTag() + " não possuir tags.");
						g.leave().queue();
					}
					, f -> {
						Helper.logger(TenthMinuteEvent.class).info("Saí do servidor " + g.getName() + " por DESCONHECIDO não possuir tags.");
						g.leave().queue();
					}
			);
		}
	}
}
