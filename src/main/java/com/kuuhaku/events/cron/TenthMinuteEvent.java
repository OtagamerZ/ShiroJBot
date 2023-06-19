/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.controller.postgresql.VoiceTimeDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.model.persistent.VoiceTime;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.PaidRole;
import com.kuuhaku.model.persistent.guild.VoiceRole;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.tuple.Pair;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TenthMinuteEvent implements Job {
	static JobDetail tenthMinute;

	@Override
	public void execute(JobExecutionContext context) {
		for (Account account : AccountDAO.getNotifiableAccounts()) {
			account.notifyVote();
		}

		List<GuildConfig> guilds = GuildDAO.getAllGuildsWithPaidRoles();
		for (GuildConfig gc : guilds) {
			Guild guild = Main.getInfo().getGuildByID(gc.getGuildId());
			if (guild == null) continue;

			Set<String> invalid = new HashSet<>();
			for (PaidRole pr : gc.getPaidRoles()) {
				Role r = guild.getRoleById(pr.getId());
				if (r == null) {
					invalid.add(pr.getId());
					continue;
				}

				Set<String> valid = pr.getUsers().keySet();
				Set<String> toExpire = pr.getExpiredUsers();
				valid.removeAll(toExpire);

				List<RestAction<?>> acts = new ArrayList<>();
				for (String s : toExpire) {
					Member m = guild.getMemberById(s);
					if (m == null) continue;

					if (!m.getRoles().contains(r))
						try {
							TextChannel tc = gc.getLevelChannel();
							acts.add(guild.removeRoleFromMember(m, r).flatMap(
									p -> gc.isLevelNotif() && tc != null,
									v -> tc.sendMessage(" O cargo **`" + r.getName() + "`** de " + m.getAsMention() + " expirou! :alarm_clock:")
							));
						} catch (HierarchyException | InsufficientPermissionException ignore) {
						}
				}


				for (String s : valid) {
					Member m = guild.getMemberById(s);
					if (m == null) continue;

					try {
						acts.add(guild.addRoleToMember(m, r));
					} catch (Exception ignore) {
					}
				}

				if (acts.isEmpty()) continue;
				RestAction.allOf(acts).mapToResult().queue();
			}

			if (!invalid.isEmpty()) {
				for (String s : invalid) {
					gc.removePaidRole(s);
				}

				GuildDAO.updateGuildSettings(gc);
			}
		}

		guilds = GuildDAO.getAllGuildsWithGeneralChannel();
		for (GuildConfig gc : guilds) {
			Guild g = Main.getInfo().getGuildByID(gc.getGuildId());
			if (g != null && !Helper.getOr(gc.getGeneralTopic(), "").isBlank()) {
				TextChannel tc = gc.getGeneralChannel();
				if (tc != null)
					try {
						tc.getManager()
								.setTopic(gc.getGeneralTopic().replace("%count%", Helper.getFancyNumber(g.getMemberCount())))
								.queue(null, t -> {
									gc.setGeneralChannel(null);
									GuildDAO.updateGuildSettings(gc);
								});
					} catch (InsufficientPermissionException e) {
						gc.setGeneralChannel(null);
						GuildDAO.updateGuildSettings(gc);
					}
				else {
					gc.setGeneralChannel(null);
					GuildDAO.updateGuildSettings(gc);
				}
			}
		}

		guilds = GuildDAO.getAllGuildsWithVoiceRoles();
		for (GuildConfig gc : guilds) {
			Guild guild = Main.getInfo().getGuildByID(gc.getGuildId());
			if (guild == null) continue;

			Set<String> invalid = new HashSet<>();
			List<VoiceTime> vts = VoiceTimeDAO.getAllVoiceTimes(guild.getId());
			for (VoiceTime vt : vts) {
				Set<VoiceRole> vroles = gc.getVoiceRoles();

				if (vroles.isEmpty()) continue;

				VoiceRole max = vroles.stream()
						.filter(vr -> vr.getTime() <= vt.getTime())
						.max(Comparator.comparingLong(VoiceRole::getTime))
						.orElse(null);

				Member m = guild.getMemberById(vt.getUid());
				if (m == null) continue;

				Pair<Role, VoiceRole> role = null;
				Set<Role> prev = new HashSet<>();
				for (VoiceRole vrole : vroles) {
					Role r = guild.getRoleById(vrole.getId());
					if (r == null) {
						invalid.add(vrole.getId());
						continue;
					}

					if (vrole.equals(max)) {
						if (!m.getRoles().contains(r)) {
							role = Pair.of(r, vrole);
						}
					} else {
						if (m.getRoles().contains(r))
							prev.add(r);
					}
				}

				if (role == null && prev.isEmpty()) continue;
				Pair<Role, VoiceRole> finalRole = role;

				TextChannel tc = gc.getLevelChannel();
				try {
					guild.modifyMemberRoles(m, role != null ? List.of(role.getLeft()) : null, prev)
							.flatMap(
									p -> gc.isLevelNotif() && tc != null && finalRole != null,
									v -> tc.sendMessage(m.getAsMention() + " ganhou o cargo **`" + finalRole.getLeft().getName() + "`** por acumular " + Helper.toStringDuration(finalRole.getRight().getTime()) + " em call! :tada:")
							)
							.queue();
				} catch (HierarchyException | InsufficientPermissionException ignore) {
				}
			}

			if (!invalid.isEmpty()) {
				for (String s : invalid) {
					gc.removeVoiceRole(s);
				}

				GuildDAO.updateGuildSettings(gc);
			}
		}

		List<MatchMakingRating> mmrs = MatchMakingRatingDAO.getRetrialMMRs();
		for (MatchMakingRating mmr : mmrs) {
			mmr.setJoins(mmr.getJoins() - 1);
			MatchMakingRatingDAO.saveMMR(mmr);
		}

		File[] temp = Main.getInfo().getTemporaryFolder().listFiles();
		assert temp != null;
		for (File file : temp) {
			try {
				BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
				if (TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - attr.creationTime().toMillis()) >= 3) {
					if (!file.delete()) {
						Helper.logger(this.getClass()).warn("Failed to delete file at " + file.toPath().getFileName());
					}
				}
			} catch (IOException ignore) {
			}
		}
	}
}
