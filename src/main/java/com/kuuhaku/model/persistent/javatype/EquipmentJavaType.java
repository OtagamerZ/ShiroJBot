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

package com.kuuhaku.model.persistent.javatype;

import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.dunhun.GearSlot;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.records.dunhun.Equipment;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import java.io.Serial;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EquipmentJavaType extends AbstractClassJavaType<Equipment> {
	@Serial
	private static final long serialVersionUID = -8552211191796240681L;

	public static final EquipmentJavaType INSTANCE = new EquipmentJavaType();

	public EquipmentJavaType() {
		super(Equipment.class, new ImmutableMutabilityPlan<>());
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getTypeConfiguration()
				.getJdbcTypeRegistry()
				.getDescriptor(Types.VARCHAR);
	}

	@Override
	public String toString(GuildMessageChannel value) {
		if (value == null) return null;

		return value.getId();
	}

	@Override
	public Equipment fromString(CharSequence id) {
		JSONObject jo = new JSONObject(id);

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

		return Equipment.fromSupplier((gs, i) -> {
			if (i < 0) {
				return gear.get(jo.getInt(gs.name()));
			}

			return gear.get(jo.getJSONArray(gs.name()).getInt(i));
		});
	}

	@Override
	public <X> X unwrap(Equipment value, Class<X> type, WrapperOptions options) {
		if (value == null) return null;

		if (String.class.isAssignableFrom(type)) {
			return type.cast(value.toString());
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> Equipment wrap(X value, WrapperOptions options) {
		return switch (value) {
			case null -> null;
			case String data -> fromString(data);
			case Equipment e -> e;
			default -> throw unknownWrap(value.getClass());
		};
	}
}
