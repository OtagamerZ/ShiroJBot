/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.guild.buttons;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.jsoup.internal.StringUtil;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "button")
public class Button {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(nullable = false, name = "parent_id")
	private ButtonMessage parent;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String role;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String emote;

	public Button() {
	}

	public Button(String role, String emote) {
		this.role = role;
		this.emote = emote;
	}

	public int getId() {
		return id;
	}

	public ButtonMessage getParent() {
		return parent;
	}

	public void setParent(ButtonMessage parent) {
		this.parent = parent;
	}

	public Role getRole(Guild g) {
		return g.getRoleById(role);
	}

	public void setRole(Role role) {
		this.role = role.getId();
	}

	public String getEmote() {
		return emote;
	}

	public void setEmote(String emote) {
		this.emote = emote;
	}

	public boolean isEmote() {
		return StringUtil.isNumeric(emote);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Button button = (Button) o;
		return id == button.id && Objects.equals(role, button.role) && Objects.equals(emote, button.emote);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, role, emote);
	}
}