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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "access_token")
public class AccessToken extends DAO<AccessToken> {
	@Id
	@Column(name = "token", nullable = false)
	private String token;

	@Column(name = "salt", nullable = false)
	private String salt;

	@Column(name = "enabled", nullable = false)
	private boolean enabled = true;

	@Column(name = "bearer", nullable = false, unique = true)
	private String bearer;

	public AccessToken() {
		byte[] salt = new byte[8];
		Constants.DEFAULT_SECURE_RNG.nextBytes(salt);

		this.salt = Hex.encodeHexString(DigestUtils.sha1(salt));
	}

	public String getToken() {
		return token;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBearer() {
		return bearer;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AccessToken that = (AccessToken) o;
		return Objects.equals(token, that.token);
	}

	@Override
	public int hashCode() {
		return Objects.hash(token);
	}
}
