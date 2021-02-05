/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent;

import com.kuuhaku.utils.Helper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "couplemultiplier")
public class CoupleMultiplier {
	@Id
	private String id;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 1.25")
	private float mult = 1.25f;

	public CoupleMultiplier(String id) {
		this.id = id;
	}

	public CoupleMultiplier() {
	}

	public String getId() {
		return id;
	}

	public float getMult() {
		return mult;
	}

	public void decrease() {
		this.mult = Helper.clamp(this.mult * 0.99f, 1.05f, 1.25f);
		;
	}
}
