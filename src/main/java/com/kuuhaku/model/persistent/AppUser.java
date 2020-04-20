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

import com.kuuhaku.Main;
import com.kuuhaku.controller.sqlite.MemberDAO;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "appuser")
public class AppUser {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String uid = "";

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String login = "";

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String password = "";

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String name = "";

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String avatar = "";

	@Column(columnDefinition = "TEXT")
	private String guilds = "";

	public AppUser() {
	}

	public void update(String uid, String name, String avatar) {
		this.uid = uid;
		this.name = name;
		this.avatar = avatar;

		List<Guild> guilds = Main.getInfo().getAPI().getGuilds().stream().filter(g -> g.getMemberById(uid) != null).collect(Collectors.toList());
		JSONArray guildArray = new JSONArray();
		guilds.forEach(g -> {
			try {
				MemberDAO.getMemberById(uid + g.getId());
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
			} catch (NoResultException ignore) {
			}
		});

		this.guilds = guildArray.toString();
	}

	public int getId() {
		return id;
	}

	public String getUid() {
		return uid;
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
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
		json.put("login", login);
		json.put("password", password);
		json.put("name", name);
		json.put("avatar", avatar);
		json.put("guilds", new JSONArray(guilds));

		return json.toString();
	}
}
