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

package com.kuuhaku.model.persistent.guild;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.awt.*;
import java.util.Objects;

@Entity
@Table(name = "colorrole")
public class ColorRole {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String name;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn("colorRoles")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private GuildConfig guildConfig;

	@Column(columnDefinition = "VARCHAR(7) NOT NULL")
	private String hex;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String id;

	public ColorRole() {
	}

	public ColorRole(String id, String hex, String name) {
		this.id = id;
		this.hex = hex;
		this.name = name;
	}

	public ColorRole(String id, Color color, String name) {
		this.id = id;
		this.hex = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public GuildConfig getGuildConfig() {
		return guildConfig;
	}

	public Color getColor() {
		return Color.decode(hex);
	}

	public void setColor(Color color) {
		this.hex = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ColorRole colorRole = (ColorRole) o;
		return Objects.equals(name, colorRole.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
