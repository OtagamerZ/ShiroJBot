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

package com.kuuhaku.model.persistent.converter;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.common.dunhun.Equipment;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.intellij.lang.annotations.Language;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Converter(autoApply = true)
public class EquipmentConverter implements AttributeConverter<Equipment, String> {
	@Override
	public String convertToDatabaseColumn(Equipment equip) {
		if (equip == null) return null;

		return equip.toString();
	}

	@Override
	public Equipment convertToEntityAttribute(@Language("JSON5") String data) {
		JSONObject jo = new JSONObject(data);

		List<Integer> ids = new ArrayList<>();
		for (Object o : jo.values()) {
			if (o instanceof Number n) {
				ids.add(n.intValue());
			} else if (o instanceof Collection<?> c) {
				for (Object n : c) {
					ids.add(((Number) n).intValue());
				}
			}
		}

		Map<Integer, Gear> gear = DAO.queryAll(Gear.class, "SELECT g FROM Gear g WHERE g.id IN ?1", ids)
				.parallelStream()
				.collect(Collectors.toMap(Gear::getId, Function.identity()));

		return new Equipment((gs, i) -> {
			if (i < 0) {
				return gear.get(jo.getInt(gs.name()));
			}

			return gear.get(jo.getJSONArray(gs.name()).getInt(i));
		});
	}
}
