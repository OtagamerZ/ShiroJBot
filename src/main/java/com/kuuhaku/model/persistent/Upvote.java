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

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "upvote")
public class Upvote {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String uid = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String name = "";

	@Column(columnDefinition = "TIMESTAMP")
	private LocalDateTime votedAt = LocalDateTime.now();

	public Upvote() {
	}

	public Upvote(String uid, String name) {
		this.uid = uid;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDateTime getVotedAt() {
		return votedAt;
	}

	public void setVotedAt(LocalDateTime votedAt) {
		this.votedAt = votedAt;
	}
}
