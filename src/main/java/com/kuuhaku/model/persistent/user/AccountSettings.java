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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.ProfileEffect;
import com.kuuhaku.model.persistent.converter.ColorConverter;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.util.API;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import com.ygimenez.json.JSONUtils;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.awt.*;
import java.util.Map;

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

	@Column(name = "bio", length = 300)
	private String bio;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "widgets", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray widgets = new JSONArray();

	@Column(name = "deck_capacity", nullable = false)
	private int deckCapacity = 2;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "aliases", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject aliases = new JSONObject();

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

	public JSONObject getAliases() {
		return aliases;
	}
}
