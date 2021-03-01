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

import javax.persistence.*;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

@Entity
@Table(name = "blacklist")
public class Blacklist {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String uid;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String blockedBy = "";

	@Temporal(TemporalType.DATE)
	private Calendar blockDate = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT-3")));

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

	public Calendar getBlockDate() {
		return blockDate;
	}

	public void setBlockDate(Calendar blockDate) {
		this.blockDate = blockDate;
	}

	public boolean canClear() {
		Calendar today = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT-3")));
		Calendar lastDaily = this.blockDate;

		return today.get(Calendar.MONTH) > lastDaily.get(Calendar.MONTH);
	}
}
