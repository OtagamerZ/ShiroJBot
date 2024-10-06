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

package com.kuuhaku.model.records;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Attributes(
		@Column(name = "str", nullable = false)
		int str,
		@Column(name = "dex", nullable = false)
		int dex,
		@Column(name = "wis", nullable = false)
		int wis,
		@Column(name = "vit", nullable = false)
		int vit
) {
	public Attributes merge(Attributes attr) {
		return new Attributes(
				str + attr.str,
				dex + attr.dex,
				wis + attr.wis,
				vit + attr.vit
		);
	}
}
