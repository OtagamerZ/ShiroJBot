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
import com.kuuhaku.util.text.Uwuifier;
import jakarta.persistence.*;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "consumable_info", schema = "dunhun")
public class LocalizedConsumable extends DAO<LocalizedConsumable> implements Serializable {
	@EmbeddedId
	private LocalizedId id;

	@Column(name = "name", nullable = false, length = SelectOption.LABEL_MAX_LENGTH)
	private String name;

	@Column(name = "description", nullable = false, length = SelectOption.DESCRIPTION_MAX_LENGTH)
	private String description;

	private transient boolean uwu = false;

	public LocalizedId getId() {
		return id;
	}

	public I18N getLocale() {
		return id.locale();
	}

	public String getName() {
		if (uwu) {
			return Uwuifier.INSTANCE.uwu(getLocale(), name);
		}

		return name;
	}

	public String getDescription() {
		if (uwu) {
			return Uwuifier.INSTANCE.uwu(getLocale(), description);
		}

		return description;
	}

	public LocalizedConsumable setUwu(boolean uwu) {
		this.uwu = uwu;
		return this;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LocalizedConsumable that = (LocalizedConsumable) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}