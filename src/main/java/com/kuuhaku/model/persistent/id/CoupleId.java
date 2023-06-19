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

package com.kuuhaku.model.persistent.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CoupleId implements Serializable {
	@Serial
	private static final long serialVersionUID = -2973502050776799631L;

	@Column(name = "first", nullable = false, unique = true)
	private String first;

	@Column(name = "second", nullable = false, unique = true)
	private String second;

	public CoupleId() {
	}

	public CoupleId(String first, String second) {
		this.first = first;
		this.second = second;
	}

	public String getFirst() {
		return first;
	}

	public String getSecond() {
		return second;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CoupleId coupleId = (CoupleId) o;
		return (Objects.equals(first, coupleId.first) && Objects.equals(second, coupleId.second))
				|| (Objects.equals(first, coupleId.second) && Objects.equals(second, coupleId.first));
	}

	@Override
	public int hashCode() {
		return Objects.hash(first, second);
	}
}