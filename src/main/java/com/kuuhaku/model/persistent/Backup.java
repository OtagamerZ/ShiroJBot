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

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
public class Backup {
	@Id
	private int id;

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String guild = "";

	@Column(columnDefinition = "LONGTEXT DEFAULT ''")
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

		JSONObject data = new JSONObject(serverData);

		if (data.isEmpty()) return;

		lastRestored = Timestamp.from(Instant.now());

		g.getChannels().forEach(c -> {
			try {
				c.delete().complete();
			} catch (ErrorResponseException ignore) {
			}
		});
		g.getCategories().forEach(c -> {
			try {
				c.delete().complete();
			} catch (ErrorResponseException ignore) {
			}
		});
		g.getRoles().forEach(c -> {
			try {
				if (!c.getName().equalsIgnoreCase("Shiro") && !c.isPublicRole()) c.delete().complete();
			} catch (ErrorResponseException ignore) {
			}
		});

		JSONArray categories = data.getJSONArray("categories");
		JSONArray roles = data.getJSONArray("roles");

		categories.forEach(c -> {
			JSONObject cat = (JSONObject) c;

			g.createCategory(cat.getString("name"))
					.setPosition(cat.getInt("index"))
					.queue(s -> {

						JSONArray channels = cat.getJSONArray("channels");

						channels.forEach(ch -> {
							JSONObject chn = (JSONObject) ch;

							if (chn.getString("type").equals("text")) {
								s.createTextChannel(chn.getString("name"))
										.setTopic(chn.has("topic") ? chn.getString("topic") : null)
										.setParent(s)
										.setPosition(chn.getInt("index"))
										.setNSFW(chn.has("nsfw") && chn.getBoolean("nsfw"))
										.queue();
							} else {
								s.createVoiceChannel(chn.getString("name")).queue();
							}
						});
					});
		});

		roles.forEach(r -> {
			JSONObject role = (JSONObject) r;

			g.createRole()
					.setName(role.getString("name"))
					.setColor(role.has("color") ? (Integer) role.get("color") : null)
					.setPermissions(role.getLong("permissions"))
					.queue();
		});
	}

	public void saveServerData(Guild g) {
		JSONObject data = new JSONObject();

		JSONArray categories = new JSONArray();

		lastBackup = Timestamp.from(Instant.now());

		g.getCategories().forEach(cat -> {
			JSONObject category = new JSONObject();

			category.put("name", cat.getName());
			category.put("index", cat.getPosition());

			JSONArray channels = new JSONArray();

			cat.getChannels().forEach(channel -> {
				JSONObject channelData = new JSONObject();

				channelData.put("name", channel.getName());
				channelData.put("index", channel.getPosition());
				channelData.put("topic", channel.getType() == ChannelType.TEXT ? ((TextChannel) channel).getTopic() : null);
				channelData.put("type", channel.getType() == ChannelType.TEXT ? "text" : "voice");
				channelData.put("nsfw", channel.getType() == ChannelType.TEXT && ((TextChannel) channel).isNSFW());

				getPermissions(channel, channelData);

				channels.put(channelData);
			});

			getPermissions(cat, category);
			category.put("channels", channels);

			categories.put(category);
		});

		JSONArray roles = new JSONArray();

		g.getRoles().forEach(r -> {
			if (!r.getName().equalsIgnoreCase("Shiro") && !r.isPublicRole()) {
				JSONObject role = new JSONObject();

				role.put("name", r.getName());
				role.put("color", r.getColor() == null ? null : r.getColor().getRGB());

				role.put("permissions", r.getPermissionsRaw());

				roles.put(role);
			}
		});

		data.put("categories", categories);
		data.put("roles", roles);

		this.serverData = data.toString();
	}

	private void getPermissions(GuildChannel channel, JSONObject channelData) {
		JSONArray permissions = new JSONArray();

		channel.getPermissionOverrides().forEach(o -> {
			if (o.isRoleOverride()) {
				JSONObject permission = new JSONObject();

				permission.put("name", Objects.requireNonNull(o.getRole()).getName());
				permission.put("allowed", o.getAllowedRaw());
				permission.put("denied", o.getDeniedRaw());

				permissions.put(permission);
			}
		});

		channelData.put("permissions", permissions);
	}

	public Timestamp getLastRestored() {
		return lastRestored;
	}
}
