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
import com.kuuhaku.model.persistent.user.Profile;
import com.kuuhaku.model.records.GuildBuff;
import com.ygimenez.json.JSONObject;
import com.ygimenez.json.JSONUtils;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

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
	private I18N locale = I18N.EN;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "gid")
	@Fetch(FetchMode.JOIN)
	private GuildSettings settings;

	@OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "gid")
	@Fetch(FetchMode.JOIN)
	private WelcomeSettings welcomeSettings;

	@OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "gid")
	@Fetch(FetchMode.JOIN)
	private GoodbyeSettings goodbyeSettings;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "buffs", nullable = false, columnDefinition = "JSONB")
	private JSONObject buffs = new JSONObject();

	@OneToMany(mappedBy = "guild", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	@OrderBy("xp DESC")
	private Set<Profile> profiles = new LinkedHashSet<>();

	public GuildConfig() {
	}

	@WhenNull
	public GuildConfig(String gid) {
		this.gid = gid;
		this.settings = new GuildSettings(gid);
		this.welcomeSettings = new WelcomeSettings(this);
		this.goodbyeSettings = new GoodbyeSettings(this);
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
		return welcomeSettings;
	}

	public GoodbyeSettings getGoodbyeSettings() {
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

		Iterator<Object> it = buffs.values().iterator();
		while (it.hasNext()) {
			GuildBuff gb = JSONUtils.fromJSON(String.valueOf(it.next()), GuildBuff.class);
			if (gb == null || gb.expired()) {
				it.remove();
				continue;
			}

			card += gb.card();
			drop += gb.drop();
			xp += gb.xp();
			rarity += gb.rarity();
		}

		return new GuildBuff(card, drop, rarity, xp);
	}

	public Set<Profile> getProfiles() {
		return profiles;
	}
}
