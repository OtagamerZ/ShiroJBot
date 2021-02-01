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

package com.kuuhaku.model.persistent;

import com.kuuhaku.controller.postgresql.BackupDAO;
import com.kuuhaku.model.common.backup.GuildCategory;
import com.kuuhaku.model.common.backup.GuildData;
import com.kuuhaku.model.common.backup.GuildRole;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Entity
@Table(name = "backup")
public class Backup {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191)")
	private String guild;

	@Column(columnDefinition = "TEXT")
	private String serverData = "";

	@Column(columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
	private Timestamp lastRestored = Timestamp.from(Instant.now());

	@Column(columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
	private Timestamp lastBackup = Timestamp.from(Instant.now());

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getGuild() {
		return guild;
	}

	public void setGuild(String guild) {
		this.guild = guild;
	}

	public void restore(Guild g) {
		if (serverData == null || serverData.isEmpty()) return;

		lastRestored = Timestamp.from(Instant.now());

		GuildData gdata = ShiroInfo.getJSONFactory().create().fromJson(serverData, GuildData.class);

		LinkedList<RestAction<?>> queue = new LinkedList<>();
		Map<Long, Role> newRoles = new HashMap<>();
		Map<GuildCategory, Category> newCategories = new HashMap<>();

		for (GuildChannel guildChannel : g.getChannels()) {
			try {
				queue.offer(guildChannel.delete());
			} catch (Exception ignore) {
			}
		}

		for (Role role : g.getRoles()) {
			try {
				if (!role.isPublicRole()) queue.offer(role.delete());
			} catch (Exception ignore) {
			}
		}

		TextChannel progress = g.createTextChannel("progresso").complete();

		progress.sendMessage("Preparando backup, isso pode demorar vários minutos, aguarde...").queue();

		for (GuildCategory guildCategory : gdata.getCategories()) {
			queue.offer(g.createCategory(guildCategory.getName())
					.map(c -> newCategories.put(guildCategory, c)));
		}

		for (GuildRole gr : gdata.getRoles()) {
			if (gr.isPublicRole()) queue.offer(g.getPublicRole()
					.getManager()
					.setPermissions(gr.getPermission())
					.map(r -> newRoles.put(gr.getOldId(), g.getPublicRole()))
			);
			else queue.offer(g.createRole()
					.setName(gr.getName())
					.setColor(gr.getColor())
					.setPermissions(gr.getPermission())
					.map(r -> newRoles.put(gr.getOldId(), r))
			);
		}

		Executors.newSingleThreadExecutor().execute(() -> {
			while (!queue.isEmpty()) {
				try {
					queue.poll().complete();

					Thread.sleep(5000);
				} catch (Exception e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			}

			progress.sendMessage("Preparação do backup conclúida.\nCriando canais...").queue();

			for (Map.Entry<GuildCategory, Category> entry : newCategories.entrySet()) {
				GuildCategory gc = entry.getKey();
				Category c = entry.getValue();
				for (Map.Entry<Long, long[]> mapEntry : gc.getPermission().entrySet()) {
					Long key = mapEntry.getKey();
					long[] value = mapEntry.getValue();
					c.putPermissionOverride(newRoles.get(key))
							.setAllow(value[0])
							.setDeny(value[1])
							.complete();
				}

				for (com.kuuhaku.model.common.backup.GuildChannel chn : gc.getChannels()) {
					try {
						if (chn.isText()) {
							TextChannel tchn = g.createTextChannel(chn.getName())
									.setNSFW(chn.isNsfw())
									.setTopic(chn.getTopic())
									.setParent(c)
									.complete();

							for (Map.Entry<Long, long[]> e : chn.getPermission().entrySet()) {
								Long k = e.getKey();
								long[] v = e.getValue();
								tchn.putPermissionOverride(newRoles.get(k))
										.setAllow(v[0])
										.setDeny(v[1])
										.complete();
							}

							Thread.sleep(5000);
						} else {
							VoiceChannel vchn = g.createVoiceChannel(chn.getName())
									.setBitrate(chn.getBitrate())
									.setUserlimit(chn.getUserLimit())
									.setParent(c)
									.complete();

							for (Map.Entry<Long, long[]> e : chn.getPermission().entrySet()) {
								Long k = e.getKey();
								long[] v = e.getValue();
								vchn.putPermissionOverride(newRoles.get(k))
										.setAllow(v[0])
										.setDeny(v[1])
										.complete();
							}

							Thread.sleep(5000);
						}
					} catch (InterruptedException e) {
						Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
					}
				}
				progress.sendMessage("Categoria `" + c.getName() + "` concluída.").queue();
			}

			long duration = lastRestored.toLocalDateTime().until(LocalDateTime.now(), ChronoUnit.MILLIS);

			progress.sendMessage("@everyone | Backup restaurado com sucesso! (Tempo de execução - " + DurationFormatUtils.formatDuration(duration, "d 'dias,' HH 'horas,' mm 'min,' ss 'seg'") + ").").queue();
		});

		BackupDAO.saveBackup(this);
	}

	public void saveServerData(Guild g) {
		lastBackup = Timestamp.from(Instant.now());
		List<GuildCategory> gcats = new ArrayList<>();
		List<GuildRole> groles = g.getRoles().stream().map(r -> new GuildRole(r.getName(), r.getColorRaw(), r.getPermissionsRaw(), r.getIdLong(), r.isPublicRole())).collect(Collectors.toList());

		for (Category cat : g.getCategories()) {
			List<com.kuuhaku.model.common.backup.GuildChannel> channels = new ArrayList<>();
			Map<Long, long[]> catperms = new HashMap<>();

			for (PermissionOverride permissionOverride : cat.getPermissionOverrides()) {
				if (permissionOverride.isRoleOverride()) {
					Role r = permissionOverride.getRole();
					assert r != null;
					catperms.put(r.getIdLong(), new long[]{permissionOverride.getAllowedRaw(), permissionOverride.getDeniedRaw()});
				}
			}
			for (GuildChannel chn : cat.getChannels()) {
				Map<Long, long[]> chnperms = new HashMap<>();
				for (PermissionOverride ovr : chn.getPermissionOverrides()) {
					if (ovr.isRoleOverride()) {
						Role r = ovr.getRole();
						assert r != null;
						chnperms.put(r.getIdLong(), new long[]{ovr.getAllowedRaw(), ovr.getDeniedRaw()});
					}
				}
				switch (chn.getType()) {
					case TEXT -> {
						TextChannel tchannel = chn;
						channels.add(new com.kuuhaku.model.common.backup.GuildChannel(tchannel.getName(), tchannel.getTopic(), chnperms, tchannel.isNSFW()));
					}
					case VOICE -> {
						VoiceChannel vchannel = (VoiceChannel) chn;
						channels.add(new com.kuuhaku.model.common.backup.GuildChannel(vchannel.getName(), chnperms, vchannel.getUserLimit(), vchannel.getBitrate()));
					}
				}
			}
			gcats.add(new GuildCategory(cat.getName(), channels, catperms));
		}

		this.serverData = ShiroInfo.getJSONFactory().create().toJson(new GuildData(gcats, groles));
		BackupDAO.saveBackup(this);
	}

	public Timestamp getLastRestored() {
		return lastRestored;
	}
}
