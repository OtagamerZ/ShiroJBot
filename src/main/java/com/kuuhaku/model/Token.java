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

package com.kuuhaku.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Token {
	@Id
	private int id;
	@Column(columnDefinition = "String default \"\"")
	private String token;
	@Column(columnDefinition = "String default \"\"")
	private String holder;
	@Column(columnDefinition = "Integer default 0")
	private int calls;

	public String getToken() {
		return token;
	}

	public String getHolder() {
		return holder;
	}

	public Token addCall() {
		calls++;
		return this;
	}
}
