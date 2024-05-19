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

package com.kuuhaku.model.persistent.guild;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.annotations.WhenNull;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.user.Profile;
import com.kuuhaku.model.records.GuildBuff;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "guild_config")
public class GuildConfig extends DAO<GuildConfig> {
	@Id
	@Column(name = "gid", nullable = false)
	private String gid;

	@Column(name = "name")
	private String name;

	@Column(name = "prefix", length = 3)
	private String prefix = Constants.DEFAULT_PREFIX;

	@Enumerated(EnumType.STRING)
	@Column(name = "locale", nullable = false)
	private I18N locale = I18N.PT;

	@OneToOne(fetch = FetchType.LAZY, cascade = ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "gid")
	@Fetch(FetchMode.JOIN)
	private GuildSettings settings;

	@OneToOne(fetch = FetchType.LAZY, cascade = ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "gid")
	@Fetch(FetchMode.JOIN)
	private WelcomeSettings welcomeSettings;

	@OneToOne(fetch = FetchType.LAZY, cascade = ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "gid")
	@Fetch(FetchMode.JOIN)
	private GoodbyeSettings goodbyeSettings;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "buffs", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject buffs = new JSONObject();

	public GuildConfig() {
	}

	@WhenNull
	public GuildConfig(String gid) {
		this.gid = gid;
		this.settings = new GuildSettings(gid);
	}

	public String getGid() {
		return gid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public I18N getLocale() {
		return locale;
	}

	public void setLocale(I18N locale) {
		this.locale = locale;
	}

	public GuildSettings getSettings() {
		return settings;
	}

	public WelcomeSettings getWelcomeSettings() {
		if (welcomeSettings == null) {
			this.welcomeSettings = new WelcomeSettings(this);
		}

		return welcomeSettings;
	}

	public GoodbyeSettings getGoodbyeSettings() {
		if (goodbyeSettings == null) {
			this.goodbyeSettings = new GoodbyeSettings(this);
		}

		return goodbyeSettings;
	}

	public JSONObject getBuffs() {
		return buffs;
	}

	public void addBuff(GuildBuff gb) {
		buffs.put(gb.id(), new JSONObject(gb));
	}

	public GuildBuff getCumBuffs() {
		double card, drop, xp, rarity;
		card = drop = xp = rarity = 0;

		for (String id : buffs.keySet()) {
			JSONObject gb = buffs.getJSONObject(id);
			if (gb.getBoolean("expired")) {
				buffs.remove(id);
				continue;
			}

			card += gb.getDouble("card");
			drop += gb.getDouble("drop");
			xp += gb.getDouble("xp");
			rarity += gb.getDouble("rarity");
		}

		return new GuildBuff(card, drop, rarity, xp);
	}

	public List<Profile> getProfiles() {
		return DAO.queryAll(Profile.class, "SELECT p FROM Profile p WHERE p.id.gid = ?1 ORDER BY p.xp DESC", gid);
	}
}
