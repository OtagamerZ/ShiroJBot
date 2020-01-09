package com.kuuhaku.model;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.json.JSONObject;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Comparator;
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
				c.delete().queue();
			} catch (ErrorResponseException ignore) {
			}
		});
		g.getCategories().forEach(c -> {
			try {
				c.delete().queue();
			} catch (ErrorResponseException ignore) {
			}
		});
		g.getRoles().forEach(c -> {
			try {
				c.delete().queue();
			} catch (ErrorResponseException ignore) {
			}
		});

		Map<String, Object> categories = data.getJSONObject("categories").toMap();
		Map<String, Object> roles = data.getJSONObject("roles").toMap();

		roles.values().forEach(r -> {
			JSONObject role = (JSONObject) r;
			g.createRole()
					.setName(role.getString("name"))
					.setColor((Integer) role.get("color"))
					.setPermissions(role.getLong("permissions"))
					.queue();
		});

		categories.values().stream().sorted(Comparator.comparingInt(c -> ((JSONObject) c).getInt("index"))).forEach(c -> {
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

				Map <String, Object> channels = cat.getJSONObject("channels").toMap();

				channels.values().stream().sorted(Comparator.comparingInt(i -> ((JSONObject) i).getInt("index"))).forEach(ch -> {
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

		JSONObject categories = new JSONObject();

		g.getCategories().forEach(cat -> {
			JSONObject category = new JSONObject();

			category.put("name", cat.getName());
			category.put("index", cat.getPosition());

			JSONObject channels = new JSONObject();

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

				channels.put(channel.getId(), channelData);
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

			categories.put(cat.getId(), category);
		});

		JSONObject roles = new JSONObject();

		g.getRoles().forEach(r -> {
			JSONObject role = new JSONObject();

			role.put("name", r.getName());
			role.put("color", r.getColor() == null ? null : r.getColor().getRGB());

			role.put("permissions", r.getPermissionsRaw());

			roles.put(r.getId(), role);
		});

		data.put("categories", categories);
		data.put("roles", roles);

		this.serverData = data.toString();
	}
}
