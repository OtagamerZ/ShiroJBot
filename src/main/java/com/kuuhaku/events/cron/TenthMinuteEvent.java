/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.VoiceTimeDAO;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.VoiceTime;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.PaidRole;
import com.kuuhaku.model.persistent.guild.VoiceRole;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.Music;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TenthMinuteEvent implements Job {
	public static JobDetail tenthMinute;

	@Override
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void execute(JobExecutionContext context) {
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

		List<GuildConfig> guilds = GuildDAO.getAllGuildsWithPaidRoles();
		for (GuildConfig gc : guilds) {
			Guild guild = Main.getInfo().getGuildByID(gc.getGuildId());
			if (guild == null) continue;

			for (PaidRole pr : gc.getPaidRoles()) {
				Role r = guild.getRoleById(pr.getId());
				if (r == null) continue;

				Set<String> valid = pr.getUsers().keySet();
				Set<String> toExpire = pr.getExpiredUsers();
				valid.removeAll(toExpire);

				List<RestAction<?>> acts = new ArrayList<>();
				for (String s : toExpire) {
					Member m = guild.getMemberById(s);
					if (m == null) continue;

					if (!m.getRoles().contains(r)) {
						TextChannel tc = gc.getLevelChannel();
						acts.add(guild.removeRoleFromMember(m, r).flatMap(
								p -> gc.isLevelNotif() && tc != null,
								v -> tc.sendMessage(" O cargo **`" + r.getName() + "`** de " + m.getAsMention() + " expirou! :alarm_clock:")
						));
					}
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

		guilds = GuildDAO.getAllGuildsWithVoiceRoles();
		for (GuildConfig gc : guilds) {
			Guild guild = Main.getInfo().getGuildByID(gc.getGuildId());
			if (guild == null) continue;

			List<VoiceTime> vts = VoiceTimeDAO.getAllVoiceTimes(guild.getId());

			for (VoiceRole vr : gc.getVoiceRoles()) {
				Role r = guild.getRoleById(vr.getId());
				if (r == null) continue;

				Set<String> valid = vts.stream()
						.filter(vt -> vt.getTime() >= vr.getTime())
						.map(VoiceTime::getUid)
						.collect(Collectors.toSet());

				List<RestAction<?>> acts = new ArrayList<>();
				for (String s : valid) {
					Member m = guild.getMemberById(s);
					if (m == null) continue;

					if (!m.getRoles().contains(r)) {
						TextChannel tc = gc.getLevelChannel();
						acts.add(guild.addRoleToMember(m, r).flatMap(
								p -> gc.isLevelNotif() && tc != null,
								v -> tc.sendMessage(m.getAsMention() + " ganhou o cargo **`" + r.getName() + "`** por acumular " + Helper.toStringDuration(vr.getTime()) + " em call! :tada:")
						));
					}
				}

				if (acts.isEmpty()) continue;
				RestAction.allOf(acts)
						.mapToResult()
						.queue();
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
}
