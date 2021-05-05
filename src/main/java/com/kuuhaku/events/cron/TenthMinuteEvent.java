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
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.SupportTier;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.PaidRole;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.Music;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import javax.persistence.NoResultException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TenthMinuteEvent implements Job {
	public static JobDetail tenthMinute;

	@Override
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void execute(JobExecutionContext context) {
		for (Guild g : Main.getJibril().getGuilds()) {
			notif(g);
		}
		for (Account account : AccountDAO.getNotifiableAccounts()) {
			account.notifyVote();
		}

		for (Guild g : Main.getShiroShards().getGuilds()) {
			GuildMusicManager gmm = Music.getGuildAudioPlayer(g, null);
			if (g.getAudioManager().isConnected() && (Objects.requireNonNull(g.getAudioManager().getConnectedChannel()).getMembers().size() < 1)) {
				g.getAudioManager().closeAudioConnection();
				gmm.scheduler.channel.sendMessage("Me deixaram sozinha no chat de voz, então eu saí também!").queue();
				gmm.scheduler.clear();
				gmm.player.destroy();
			}
		}

		List<GuildConfig> guilds = GuildDAO.getAllGuildsWithExceedRoles();
		List<ExceedMember> ems = ExceedDAO.getExceedMembers();
		String[] exNames = {"imanity", "ex-machina", "exmachina", "flugel", "flügel", "werebeast", "elf", "seiren"};

		for (GuildConfig gc : guilds) {
			Guild guild = Main.getInfo().getGuildByID(gc.getGuildId());
			if (guild == null) continue;

			List<Member> mbs = guild.getMembers()
					.stream()
					.filter(m -> m != null && ems.stream().anyMatch(em -> em.getUid().equals(m.getId())))
					.collect(Collectors.toList());

			Map<ExceedEnum, List<Role>> roles = new HashMap<>();
			List<Pair<String, Role>> addRoles = guild.getRoles()
					.stream()
					.filter(r -> r.getPosition() < guild.getSelfMember().getRoles().get(0).getPosition())
					.filter(r -> Helper.containsAny(StringUtils.stripAccents(r.getName()), exNames))
					.map(r -> {
						String name = Arrays.stream(exNames).filter(s -> Helper.containsAny(StringUtils.stripAccents(r.getName()), s)).findFirst().orElse(null);
						return Pair.of(name, r);
					})
					.filter(p -> p.getKey() != null)
					.collect(Collectors.toList());

			for (Pair<String, Role> addRole : addRoles) {
				ExceedEnum ee = ExceedEnum.getByName(addRole.getLeft());
				roles.computeIfAbsent(ee, k -> new ArrayList<>()).add(addRole.getRight());
			}

			List<AuditableRestAction<Void>> acts = new ArrayList<>();
			for (Member mb : mbs) {
				ExceedEnum ex = ExceedEnum.getByName(ExceedDAO.getExceed(mb.getId()));

				if (ex != null) {
					List<Role> validRoles = roles.get(ex);
					List<Role> invalidRoles = roles.entrySet()
							.stream()
							.filter(e -> !e.getKey().equals(ex))
							.map(Map.Entry::getValue)
							.flatMap(List::stream)
							.collect(Collectors.toList());

					acts.add(guild.modifyMemberRoles(mb, validRoles, invalidRoles));
				} else {
					List<Role> invalidRoles = roles.values()
							.stream()
							.flatMap(List::stream)
							.collect(Collectors.toList());

					acts.add(guild.modifyMemberRoles(mb, null, invalidRoles));
				}
			}

			if (acts.isEmpty()) continue;
			RestAction.allOf(acts)
					.mapToResult()
					.queue();
		}

		guilds = GuildDAO.getAllGuildsWithPaidRoles();
		for (GuildConfig gc : guilds) {
			Guild guild = Main.getInfo().getGuildByID(gc.getGuildId());
			if (guild == null) continue;

			for (PaidRole pr : gc.getPaidRoles()) {
				Role r = guild.getRoleById(pr.getId());
				if (r == null) continue;

				Set<String> valid = pr.getUsers().keySet();
				Set<String> toExpire = pr.getExpiredUsers();
				valid.removeAll(toExpire);

				List<AuditableRestAction<Void>> acts = new ArrayList<>();
				for (String s : toExpire) {
					Member m = guild.getMemberById(s);
					if (m == null) continue;

					acts.add(guild.removeRoleFromMember(m, r));
				}
				for (String s : valid) {
					Member m = guild.getMemberById(s);
					if (m == null) continue;

					acts.add(guild.addRoleToMember(m, r));
				}

				if (acts.isEmpty()) continue;
				RestAction.allOf(acts)
						.mapToResult()
						.queue();
			}
		}

		Guild supportServer = Main.getInfo().getGuildByID(ShiroInfo.getSupportServerID());
		Role seniorRole = supportServer.getRoleById("815429753058230292");
		Role supRole = supportServer.getRoleById("738837622805364876");

		for (Map.Entry<String, SupportTier> sup : ShiroInfo.getSupports().entrySet()) {
			if (seniorRole != null && sup.getValue() == SupportTier.SENIOR)
				supportServer.addRoleToMember(sup.getKey(), seniorRole);
			if (supRole != null)
				supportServer.addRoleToMember(sup.getKey(), supRole);
		}


		for (GuildConfig gc : GuildDAO.getAllGuildsWithGeneralChannel()) {
			Guild g = Main.getInfo().getGuildByID(gc.getGuildId());
			if (g != null && !Helper.getOr(gc.getGeneralTopic(), "").isBlank()) {
				TextChannel tc = gc.getGeneralChannel();
				if (tc != null)
					try {
						tc.getManager()
								.setTopic(gc.getGeneralTopic().replace("%count%", Helper.getFancyNumber(g.getMemberCount(), false)))
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

		File[] temp = Main.getInfo().getTemporaryFolder().listFiles();
		assert temp != null;
		for (File file : temp) {
			try {
				BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
				if (TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - attr.creationTime().toMillis()) >= 3)
					file.delete();
			} catch (IOException ignore) {
			}
		}
	}

	private static void notif(Guild g) {
		try {
			if (!TagDAO.getTagById(g.getOwnerId()).isBeta() && !ShiroInfo.getDevelopers().contains(g.getOwnerId())) {
				g.retrieveOwner().queue(o -> {
							Helper.logger(TenthMinuteEvent.class).info("Saí do servidor " + g.getName() + " por " + o.getUser().getAsTag() + " não estar na lista de acesso beta.");
							g.leave().queue();
						}
						, f -> {
							Helper.logger(TenthMinuteEvent.class).info("Saí do servidor " + g.getName() + " por DESCONHECIDO não estar na lista de acesso beta.");
							g.leave().queue();
						}
				);
			}
		} catch (NoResultException e) {
			g.retrieveOwner().queue(o -> {
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
