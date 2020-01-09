package com.kuuhaku.model;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.Entity;
import javax.persistence.Id;
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
				if (g.getChannels().contains(c)) {
					c.delete().queue();
				}
			} catch (ErrorResponseException ignore) {
			}
		});
		g.getCategories().forEach(c -> {
			try {
				if (g.getCategories().contains(c)) {
					c.delete().queue();
				}
			} catch (ErrorResponseException ignore) {
			}
		});
		g.getRoles().forEach(c -> {
			try {
				if (g.getRoles().contains(c) && !c.getName().equalsIgnoreCase("Shiro") && !c.isPublicRole()) {
					c.delete().queue();
				}
			} catch (ErrorResponseException ignore) {
			}
		});

		JSONArray categories = data.getJSONArray("categories");
		JSONArray roles = data.getJSONArray("roles");

		roles.forEach(r -> {
			JSONObject role = (JSONObject) r;
			g.createRole()
					.setName(role.getString("name"))
					.setColor((Integer) role.get("color"))
					.setPermissions(role.getLong("permissions"))
					.queue();
		});

		categories.forEach(c -> {
			JSONObject cat = (JSONObject) c;

			g.createCategory(cat.getString("name"))
					.setPosition(cat.getInt("index"))
					.queue(s -> {
						cat.getJSONObject("permissions").toMap().values().forEach(o -> {
							JSONObject override = (JSONObject) o;

							s.createPermissionOverride(override.getBoolean("role") ? g.getRolesByName(override.getString("nameOrId"), true).get(0) : Objects.requireNonNull(g.getMemberById(override.getString("nameOrId"))))
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
										.setTopic(chn.getString("topic"))
										.setParent(s)
										.setPosition(chn.getInt("index"))
										.queue(textChannel -> channel[0] = textChannel);
							} else {
								s.createVoiceChannel(chn.getString("name")).queue(voiceChannel -> channel[0] = voiceChannel);
							}

							chn.getJSONObject("permissions").toMap().values().forEach(o -> {
								JSONObject override = (JSONObject) o;

								channel[0].createPermissionOverride(override.getBoolean("role") ? g.getRolesByName(override.getString("nameOrId"), true).get(0) : Objects.requireNonNull(g.getMemberById(override.getString("nameOrId"))))
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

				JSONObject permissions = new JSONObject();

				channel.getPermissionOverrides().forEach(o -> {
					JSONObject permission = new JSONObject();

					permission.put("id", o.isRoleOverride() ? Objects.requireNonNull(o.getRole()).getId() : Objects.requireNonNull(o.getMember()).getId());
					permission.put("role", o.isRoleOverride());
					permission.put("allowed", o.getAllowedRaw());
					permission.put("denied", o.getDeniedRaw());

					permissions.put(permission.getString("id"), permission);
				});

				channelData.put("permissions", permissions);

				channels.put(channelData);
			});

			JSONObject permissions = new JSONObject();

			cat.getPermissionOverrides().forEach(o -> {
				JSONObject permission = new JSONObject();

				permission.put("nameOrId", o.isRoleOverride() ? Objects.requireNonNull(o.getRole()).getName() : Objects.requireNonNull(o.getMember()).getId());
				permission.put("role", o.isRoleOverride());
				permission.put("allowed", o.getAllowedRaw());
				permission.put("denied", o.getDeniedRaw());

				permissions.put(permission.getString("nameOrId"), permission);
			});

			category.put("permissions", permissions);
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
}
