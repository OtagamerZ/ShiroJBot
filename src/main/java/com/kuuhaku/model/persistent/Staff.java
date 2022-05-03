/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.enums.StaffType;

import javax.persistence.*;

@Entity
@Table(name = "staff")
public class Staff {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Enumerated(value = EnumType.STRING)
	private StaffType type = StaffType.NONE;

	public String getUid() {
		return uid;
	}

	public StaffType getType() {
		return type;
	}
}
