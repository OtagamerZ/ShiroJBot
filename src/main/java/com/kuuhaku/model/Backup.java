package com.kuuhaku.model;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
public class Backup {
	@Id
	private int id;
	private String guild;
	private String serverData;

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

	public void recoverServerData(Guild g) {
		if (serverData == null || serverData.isEmpty()) return;

		JSONObject data = new JSONObject(serverData);

		if (data.isEmpty()) return;

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

		Map<String, Role> createdRoles = new HashMap<>();

		roles.forEach(r -> {
			JSONObject role = (JSONObject) r;
			createdRoles.put(((JSONObject) r).getString("name"), g.createRole()
					.setName(role.getString("name"))
					.setColor(role.has("color") ? (Integer) role.get("color") : null)
					.setPermissions(role.getLong("permissions"))
					.complete());
		});

		categories.forEach(c -> {
			JSONObject cat = (JSONObject) c;

			g.createCategory(cat.getString("name"))
					.setPosition(cat.getInt("index"))
					.queue(s -> {
						cat.getJSONArray("permissions").forEach(o -> {
							JSONObject override = (JSONObject) o;

							s.createPermissionOverride(createdRoles.get(override.getString("name")))
									.setAllow(override.getLong("allowed"))
									.setDeny(override.getLong("denied"))
									.queue();
						});

						JSONArray channels = cat.getJSONArray("channels");

						channels.forEach(ch -> {
							JSONObject chn = (JSONObject) ch;

							final GuildChannel[] channel = new GuildChannel[1];

							if (chn.getString("type").equals("text")) {
								s.createTextChannel(chn.getString("name"))
										.setTopic(chn.has("topic") ? chn.getString("topic") : null)
										.setParent(s)
										.setPosition(chn.getInt("index"))
										.setNSFW(chn.has("nsfw") && chn.getBoolean("nsfw"))
										.queue(textChannel -> channel[0] = textChannel);
							} else {
								s.createVoiceChannel(chn.getString("name")).queue(voiceChannel -> channel[0] = voiceChannel);
							}

							chn.getJSONArray("permissions").forEach(o -> {
								JSONObject override = (JSONObject) o;

								channel[0].createPermissionOverride(createdRoles.get(override.getString("name")))
										.setAllow(override.getLong("allowed"))
										.setDeny(override.getLong("denied"))
										.queue();
							});
						});
					});
		});
	}

	public void saveServerData(Guild g) {
		JSONObject data = new JSONObject();

		JSONArray categories = new JSONArray();

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
}
