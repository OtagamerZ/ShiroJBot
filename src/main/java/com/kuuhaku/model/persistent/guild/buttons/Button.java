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

package com.kuuhaku.model.persistent.guild.buttons;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.jsoup.internal.StringUtil;

import javax.persistence.*;

@Entity
@Table(name = "button")
public class Button {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "parent_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
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

	public Button(ButtonMessage parent, String role, String emote) {
		this.parent = parent;
		this.role = role;
		this.emote = emote;
	}

	public int getId() {
		return id;
	}

	public ButtonMessage getParent() {
		return parent;
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
}
