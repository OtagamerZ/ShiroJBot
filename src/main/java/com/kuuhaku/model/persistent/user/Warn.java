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
import com.kuuhaku.model.persistent.id.WarnId;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_warn")
public class Warn extends DAO<Warn> {
	@EmbeddedId
	private WarnId id;

	@Column(name = "reason", nullable = false, columnDefinition = "TEXT")
	private String reason;

	@Column(name = "issuer", nullable = false)
	private String issuer;

	@Column(name = "pardoner")
	private String pardoner;

	@Column(name = "occurence", nullable = false)
	private ZonedDateTime occurence = ZonedDateTime.now(ZoneId.of("GMT-3"));

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumns({
			@PrimaryKeyJoinColumn(name = "profile_uid", referencedColumnName = "uid"),
			@PrimaryKeyJoinColumn(name = "profile_gid", referencedColumnName = "gid")
	})
	@Fetch(FetchMode.JOIN)
	private Profile profile;

	public Warn() {
	}

	public Warn(Profile profile, User issuer, String reason) {
		this.id = new WarnId(profile.getId().getGid(), profile.getId().getUid());
		this.reason = reason;
		this.issuer = issuer.getId();
		this.profile = profile;
	}

	public WarnId getId() {
		return id;
	}

	public String getReason() {
		return reason;
	}

	public String getIssuer() {
		return issuer;
	}

	public String getPardoner() {
		return pardoner;
	}

	public void setPardoner(User pardoner) {
		this.pardoner = pardoner.getId();
	}

	public ZonedDateTime getOccurence() {
		return occurence;
	}

	public Profile getProfile() {
		return profile;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Warn warn = (Warn) o;
		return Objects.equals(id, warn.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
