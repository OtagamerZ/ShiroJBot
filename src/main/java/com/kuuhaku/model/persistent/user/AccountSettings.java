/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.enums.ProfileEffect;
import com.kuuhaku.model.persistent.converter.ColorConverter;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.util.Utils;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.awt.*;

@Entity
@Table(name = "account_settings")
public class AccountSettings extends DAO<AccountSettings> {
	public static final long MAX_BG_SIZE = 4 * 1024 * 1024;

	@Id
	@Column(name = "uid", nullable = false)
	private String uid;

	@Column(name = "color", nullable = false, length = 6)
	@Convert(converter = ColorConverter.class)
	private Color color = Color.BLACK;

	@Column(name = "background")
	private String background;

	@Enumerated(EnumType.STRING)
	@Column(name = "effect", nullable = false)
	private ProfileEffect effect = ProfileEffect.NONE;

	@Column(name = "bio")
	private String bio;

	@Column(name = "widgets", nullable = false, columnDefinition = "JSONB")
	private JSONArray widgets = new JSONArray();

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
		return Utils.getOr(background, "https://i.ibb.co/F5rkrmR/cap-No-Game-No-Life-S01-E01-Beginner-00-11-41-04.jpg");
	}

	public void setBackground(String background) {
		this.background = background;
	}

	public ProfileEffect getEffect() {
		return effect;
	}

	public void setEffect(ProfileEffect effect) {
		this.effect = effect;
	}

	public String getBio() {
		return Utils.getOr(bio, "");
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public JSONArray getWidgets() {
		return widgets;
	}

	public int getDeckCapacity() {
		return deckCapacity;
	}

	public void setDeckCapacity(int deckCapacity) {
		this.deckCapacity = deckCapacity;
	}
}
