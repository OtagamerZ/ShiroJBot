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

import com.kuuhaku.utils.Helper;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "lobby")
public class Lobby {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String owner = "";

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> players = new HashSet<>();

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String name = "";

	@Column(columnDefinition = "INT NOT NULL DEFAULT 2")
	private int maxPlayers = 2;

	@Column(columnDefinition = "VARCHAR(64) NOT NULL DEFAULT ''")
	private String password = "";

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getOwner() {
		return owner;
	}

	public Lobby setOwner(String owner) {
		this.owner = owner;
		return this;
	}

	public Set<String> getPlayers() {
		return players;
	}

	public void setPlayers(Set<String> players) {
		this.players = players;
	}

	public void addPlayer(String player) {
		players.add(player);
	}

	public void removePlayer(String player) {
		players.remove(player);
	}

	public String getName() {
		return name;
	}

	public Lobby setName(String name) {
		this.name = name;
		return this;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public Lobby setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
		return this;
	}

	public boolean hasPassword() {
		return !password.isBlank();
	}

	public boolean checkPassword(String pass) {
		return password.equals(Helper.hash(pass.trim().getBytes(StandardCharsets.UTF_8), "SHA-256"));
	}

	public Lobby setPassword(String password) {
		this.password = Helper.hash(password.trim().getBytes(StandardCharsets.UTF_8), "SHA-256");
		return this;
	}
}
