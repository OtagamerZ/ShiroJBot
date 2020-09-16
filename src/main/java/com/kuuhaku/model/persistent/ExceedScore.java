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

import com.kuuhaku.model.enums.ExceedEnum;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "exceedscore")
public class ExceedScore {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Enumerated(EnumType.STRING)
	private ExceedEnum exceed;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long points = 0;

	@Column(columnDefinition = "DATE")
	private LocalDate timestamp = LocalDate.now();

	public ExceedScore() {
	}

	public ExceedScore(ExceedEnum exceed, long points, LocalDate timestamp) {
		this.exceed = exceed;
		this.points = points;
		this.timestamp = timestamp;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ExceedEnum getExceed() {
		return exceed;
	}

	public void setExceed(ExceedEnum exceed) {
		this.exceed = exceed;
	}

	public long getPoints() {
		return points;
	}

	public void setPoints(long points) {
		this.points = points;
	}

	public LocalDate getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDate when) {
		this.timestamp = when;
	}
}
