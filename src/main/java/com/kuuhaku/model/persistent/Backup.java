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
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


	}

	public void saveServerData(Guild g) {
		lastBackup = Timestamp.from(Instant.now());

		List<GuildCategory> gcats = new ArrayList<>();
		List<GuildRole> groles = g.getRoles().stream().map(r -> new GuildRole(r.getName(), r.getColorRaw(), r.getPermissionsRaw(), r.getIdLong())).collect(Collectors.toList());
		List<String> gmembers = g.getMembers().stream().map(Member::getId).collect(Collectors.toList());

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
