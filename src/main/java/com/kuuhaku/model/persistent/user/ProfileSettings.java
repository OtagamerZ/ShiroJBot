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
import com.kuuhaku.model.persistent.id.ProfileId;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "profile_settings")
public class ProfileSettings extends DAO<ProfileSettings> {
	@EmbeddedId
	private ProfileId id;

	@Column(name = "name_alias")
	private String nameAlias;

	@Column(name = "avatar_alias")
	private String avatarAlias;

	public ProfileSettings() {
	}

	public ProfileSettings(ProfileId id) {
		this.id = id;
	}

	public ProfileId getId() {
		return id;
	}

	public String getNameAlias() {
		return nameAlias;
	}

	public void setNameAlias(String nameAlias) {
		this.nameAlias = nameAlias;
	}

	public String getAvatarAlias() {
		return avatarAlias;
	}

	public void setAvatarAlias(String avatarAlias) {
		this.avatarAlias = avatarAlias;
	}
}
