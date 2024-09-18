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

import com.kuuhaku.model.enums.dunhun.GearSlot;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public record GearStats(
		@Column(name = "req_level", nullable = false)
		int reqLevel,
		@Column(name = "attack", nullable = false)
		int attack,
		@Column(name = "defense", nullable = false)
		int defense,
		@Enumerated(EnumType.STRING)
		@Column(name = "slot", nullable = false)
		GearSlot slot,
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "tags", nullable = false, columnDefinition = "JSONB")
		@Convert(converter = JSONArrayConverter.class)
		JSONArray tags
) {
}
