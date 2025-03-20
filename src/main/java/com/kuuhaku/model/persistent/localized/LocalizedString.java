/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.localized;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.id.LocalizedId;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "locale_string", schema = "shiro")
public class LocalizedString extends DAO<LocalizedString> implements Serializable {
	@EmbeddedId
	private LocalizedId id;

	@Column(name = "value", nullable = false, columnDefinition = "TEXT")
	private String value;

	public LocalizedId getId() {
		return id;
	}

	public I18N getLocale() {
		return id.locale();
	}

	public String getValue() {
		return value;
	}

	public static String get(I18N locale, String key) {
		return get(locale, key, key);
	}

	public static String get(I18N locale, String key, String def) {
		if (key == null) return def;

		LocalizedString ls = DAO.find(LocalizedString.class, new LocalizedId(key.toLowerCase(), locale));
		if (ls == null) return def;

		return ls.getValue();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LocalizedString that = (LocalizedString) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
