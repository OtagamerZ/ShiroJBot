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

package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.dunhun.Affix;
import com.kuuhaku.model.persistent.dunhun.GearType;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;

@Embeddable
public record GearStats(
		@Column(name = "req_level", nullable = false)
		int reqLevel,
		@Column(name = "attack", nullable = false)
		int attack,
		@Column(name = "defense", nullable = false)
		int defense,
		@Column(name = "critical", nullable = false)
		float critical,
		@Embedded
		Attributes requirements,
		@ManyToOne
		@JoinColumn(name = "gear_type", nullable = false)
		GearType gearType,
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "tags", nullable = false, columnDefinition = "JSONB")
		@Convert(converter = JSONArrayConverter.class)
		JSONArray tags,
		@ManyToOne
		@JoinColumn(name = "implicit_id")
		Affix implicit,
		@Column(name = "weight", nullable = false)
		int weight
) implements Serializable {
	public JSONArray allTags() {
		JSONArray tags = new JSONArray();
		tags.addAll(gearType.getTags());

		for (Object o : tags()) {
			String t = (String) o;
			if (t.startsWith("-")) {
				tags.remove(t.substring(1));
			} else {
				tags.add(t);
			}
		}

		return tags;
	}
}
