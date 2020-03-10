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

package com.kuuhaku.model.persistent;

import com.kuuhaku.model.common.backup.GuildCategory;
import com.kuuhaku.model.common.backup.GuildData;
import com.kuuhaku.model.common.backup.GuildRole;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.RoleAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Entity
public class Backup {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191)")
	private String guild;

	@Column(columnDefinition = "LONGTEXT")
	private String serverData = "";

	@Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private Timestamp lastRestored;

	@Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private Timestamp lastBackup;

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

		LinkedList<RestAction> queue = new LinkedList<>();
		Map<Long, Role> newRoles = new HashMap<>();
		Map<GuildCategory, Category> newCategories = new HashMap<>();

		LinkedList<Long> oldIDs = new LinkedList<>();
		LinkedList<GuildCategory> oldCategories = new LinkedList<>();

		g.getChannels().forEach(chn -> {
			try {
				queue.offer(chn.delete());
			} catch (Exception ignore) {
			}
		});
		g.getRoles().forEach(r -> {
			try {
				queue.offer(r.delete());
			} catch (Exception ignore) {
			}
		});

		gdata.getRoles().forEach(gr -> {
			queue.offer(g.createRole()
					.setName(gr.getName())
					.setColor(gr.getColor())
					.setPermissions(gr.getPermission())
			);
			oldIDs.offer(gr.getOldId());
		});

		gdata.getCategories().forEach(gc -> {
			queue.offer(g.createCategory(gc.getName()));
			oldCategories.offer(gc);
		});


		Executors.newSingleThreadExecutor().execute(() -> {
			while (!queue.isEmpty()) {
				try {
					RestAction act = queue.poll();
					if (act instanceof RoleAction) {
						Role r = ((RoleAction) act).complete();
						newRoles.put(oldIDs.poll(), r);
					} else if (act instanceof ChannelAction) {
						Category c = ((ChannelAction<Category>) act).complete();
						newCategories.put(oldCategories.poll(), c);
					} else {
						act.complete();
					}

					Thread.sleep(500);
				} catch (InterruptedException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			}

			newCategories.forEach((gc, c) -> {
				gc.getPermission().forEach((k, v) -> c.putPermissionOverride(newRoles.get(k))
						.setAllow(v[0])
						.setDeny(v[1])
						.complete()
				);

				gc.getChannels().forEach(chn -> {
					try {
						if (chn.isText()) {
							TextChannel tchn = g.createTextChannel(chn.getName())
									.setNSFW(chn.isNsfw())
									.setTopic(chn.getTopic())
									.complete();

							chn.getPermission().forEach((k, v) -> tchn.putPermissionOverride(newRoles.get(k))
									.setAllow(v[0])
									.setDeny(v[1])
									.complete()
							);

							Thread.sleep(500);
						} else {
							VoiceChannel vchn = g.createVoiceChannel(chn.getName())
									.setBitrate(chn.getBitrate())
									.setUserlimit(chn.getUserLimit())
									.complete();

							chn.getPermission().forEach((k, v) -> vchn.putPermissionOverride(newRoles.get(k))
									.setAllow(v[0])
									.setDeny(v[1])
									.complete()
							);

							Thread.sleep(500);
						}
					} catch (InterruptedException e) {
						Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
					}
				});
			});
		});
	}

	public void saveServerData(Guild g) {
		lastBackup = Timestamp.from(Instant.now());
		List<GuildCategory> gcats = new ArrayList<>();
		List<GuildRole> groles = g.getRoles().stream().filter(r -> !r.isPublicRole()).map(r -> new GuildRole(r.getName(), r.getColorRaw(), r.getPermissionsRaw(), r.getIdLong())).collect(Collectors.toList());
		List<String> gmembers = g.getMembers().stream().map(m -> m.getUser().getAsTag()).collect(Collectors.toList());

		g.getCategories().forEach(cat -> {
			List<com.kuuhaku.model.common.backup.GuildChannel> channels = new ArrayList<>();
			Map<Long, long[]> catperms = new HashMap<>();

			cat.getPermissionOverrides().forEach(ovr -> {
				if (ovr.isRoleOverride()) {
					Role r = ovr.getRole();
					assert r != null;
					catperms.put(r.getIdLong(), new long[]{ovr.getAllowedRaw(), ovr.getDeniedRaw()});
				}
			});
			cat.getChannels().forEach(chn -> {
				Map<Long, long[]> chnperms = new HashMap<>();
				chn.getPermissionOverrides().forEach(ovr -> {
					if (ovr.isRoleOverride()) {
						Role r = ovr.getRole();
						assert r != null;
						chnperms.put(r.getIdLong(), new long[]{ovr.getAllowedRaw(), ovr.getDeniedRaw()});
					}
				});
				switch (chn.getType()) {
					case TEXT:
						TextChannel tchannel = (TextChannel) chn;
						channels.add(new com.kuuhaku.model.common.backup.GuildChannel(tchannel.getName(), tchannel.getTopic(), chnperms, tchannel.isNSFW()));
						break;
					case VOICE:
						VoiceChannel vchannel = (VoiceChannel) chn;
						channels.add(new com.kuuhaku.model.common.backup.GuildChannel(vchannel.getName(), chnperms, vchannel.getUserLimit(), vchannel.getBitrate()));
						break;
				}
			});
			gcats.add(new GuildCategory(cat.getName(), channels, catperms));
		});

		this.serverData = ShiroInfo.getJSONFactory().create().toJson(new GuildData(gcats, groles, gmembers));
	}

	public Timestamp getLastRestored() {
		return lastRestored;
	}
}
