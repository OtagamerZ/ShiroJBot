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

@Entity
@Table(name = "token")
public class Token {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String uid = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String token = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String holder = "";

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int calls = 0;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean disabled = false;

	public Token(String token, String holder, String uid) {
		this.token = token;
		this.holder = holder;
		this.uid = uid;
	}

	public Token() {
	}

	public String getToken() {
		return token;
	}

	public String getHolder() {
		return holder;
	}

	public String getUid() {
		return uid;
	}

	public Token addCall() {
		calls++;
		return this;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void disable() {
		this.disabled = true;
	}
}
