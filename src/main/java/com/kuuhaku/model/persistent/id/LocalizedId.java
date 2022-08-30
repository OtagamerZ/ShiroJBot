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

package com.kuuhaku.model.persistent.id;

import com.kuuhaku.model.enums.I18N;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class LocalizedId implements Serializable {
	@Serial
	private static final long serialVersionUID = -6914298265904559282L;

	@Column(name = "id", nullable = false)
	private String id;

	@Enumerated(EnumType.STRING)
	@Column(name = "locale", nullable = false, length = 2)
	private I18N locale;

	public LocalizedId() {
	}

	public LocalizedId(String id, I18N locale) {
		this.id = id;
		this.locale = locale;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public I18N getLocale() {
		return locale;
	}

	public void setLocale(I18N locale) {
		this.locale = locale;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LocalizedId that = (LocalizedId) o;
		return Objects.equals(id, that.id) && locale == that.locale;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, locale);
	}
}