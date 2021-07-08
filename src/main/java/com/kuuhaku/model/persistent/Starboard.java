/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import net.dv8tion.jda.api.entities.Message;

import javax.persistence.*;

@Entity
@Table(name = "starboard")
public class Starboard {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String guild = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String message = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String author = "";

	public Starboard(Message msg) {
		this.guild = msg.getGuild().getId();
		this.message = msg.getId();
		this.author = msg.getAuthor().getId();
	}

	public Starboard() {
	}

	public int getId() {
		return id;
	}

	public String getGuild() {
		return guild;
	}

	public String getMessage() {
		return message;
	}

	public String getAuthor() {
		return author;
	}
}
