package com.kuuhaku.model;

import com.kuuhaku.Main;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
public class AppUser {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private String uid;
	private String name;
	private String avatar;
	private String guilds;

	public AppUser() {
	}

	public AppUser(String uid, String name, String avatar) {
		this.uid = uid;
		this.name = name;
		this.avatar = avatar;

		List<Guild> guilds = Main.getInfo().getAPI().getGuilds().stream().filter(g -> g.getMemberById(uid) != null).collect(Collectors.toList());
		JSONArray guildArray = new JSONArray();
		guilds.forEach(g -> {
			JSONObject json = new JSONObject();
			json.put("id", g.getId());
			json.put("name", g.getName());
			json.put("icon", g.getIconUrl());

			JSONObject user = new JSONObject();
			user.put("profileId", uid + g.getId());
			user.put("nickname", Objects.requireNonNull(g.getMemberById(uid)).getNickname());
			user.put("isAdmin", Objects.requireNonNull(g.getMemberById(uid)).hasPermission(Permission.MANAGE_SERVER));

			json.put("user", user);

			guildArray.put(json);
		});

		this.guilds = guildArray.toString();
	}

	public int getId() {
		return id;
	}

	public String getUid() {
		return uid;
	}

	public String getName() {
		return name;
	}

	public String getAvatar() {
		return avatar;
	}

	public String getGuilds() {
		return guilds;
	}

	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("uid", uid);
		json.put("name", name);
		json.put("avatar", avatar);
		json.put("guilds", guilds);

		return json.toString();
	}
}
