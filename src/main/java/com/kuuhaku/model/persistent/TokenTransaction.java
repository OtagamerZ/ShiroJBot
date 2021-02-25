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

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "tokentransaction")
public class TokenTransaction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String to = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String from = "";

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int value = 0;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String date = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3")).format(Helper.dateformat);

	public TokenTransaction(String to, String from, int value) {
		this.to = to;
		this.from = from;
		this.value = value;
	}

	public TokenTransaction() {
	}

	public int getId() {
		return id;
	}

	public String getTo() {
		return to;
	}

	public String getFrom() {
		return from;
	}

	public int getValue() {
		return value;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
}
