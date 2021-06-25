/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "blacklist")
public class Blacklist {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String blockedBy = "";

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime blockDate = ZonedDateTime.now(ZoneId.of("GMT-3"));

	public Blacklist(String uid, String by) {
		this.uid = uid;
		this.blockedBy = by;
	}

	public Blacklist() {
	}

	public String getUid() {
		return uid;
	}

	public String getBlockedBy() {
		return blockedBy;
	}

	public ZonedDateTime getBlockDate() {
		return blockDate;
	}

	public void setBlockDate(ZonedDateTime blockDate) {
		this.blockDate = blockDate;
	}

	public boolean canClear() {
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));

		return today.isAfter(this.blockDate.plusMonths(1));
	}
}
