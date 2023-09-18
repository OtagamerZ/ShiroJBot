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

package com.kuuhaku.model.persistent.shiro;

import com.kuuhaku.controller.DAO;
import jakarta.persistence.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "analytics")
public class Analytics extends DAO<Analytics> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "source", nullable = false)
	private String source;

	@Column(name = "label", nullable = false)
	private String label;

	@Column(name = "value", nullable = false)
	private int value;

	@Column(name = "moment", nullable = false)
	private ZonedDateTime moment = ZonedDateTime.now(ZoneId.of("GMT-3"));

	public int getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public double getValue() {
		return value;
	}

	public ZonedDateTime getMoment() {
		return moment;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Analytics analytics = (Analytics) o;
		return id == analytics.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	public static void register(String source, String label, int value) {
		Analytics a = new Analytics();
		a.source = source;
		a.label = label;
		a.value = value;
		a.save();
	}
}
