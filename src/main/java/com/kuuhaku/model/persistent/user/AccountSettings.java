/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.converter.ColorConverter;

import jakarta.persistence.*;
import java.awt.*;

@Entity
@Table(name = "account_settings")
public class AccountSettings extends DAO<AccountSettings> {
	@Id
	@Column(name = "uid", nullable = false)
	private String uid;

	@Column(name = "color", nullable = false, length = 6)
	private Color color = new Color(0);

	@Column(name = "background")
	private String background;

	@Column(name = "bio")
	private String bio;

	@Column(name = "deck_capacity", nullable = false)
	private int deckCapacity = 2;

	public AccountSettings() {
	}

	public AccountSettings(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public String getBackground() {
		return background;
	}

	public void setBackground(String background) {
		this.background = background;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public int getDeckCapacity() {
		return deckCapacity;
	}

	public void setDeckCapacity(int deckCapacity) {
		this.deckCapacity = deckCapacity;
	}
}
