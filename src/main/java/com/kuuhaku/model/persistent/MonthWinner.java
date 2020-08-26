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
import java.time.LocalDate;

@Entity
@Table(name = "monthwinner")
public class MonthWinner {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String exceed = "";

	@Column(columnDefinition = "DATE")
	private LocalDate expiry = LocalDate.now().plusWeeks(1);

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long imanityPoints = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long seirenPoints = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long werebeastPoints = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long elfPoints = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long exmachinaPoints = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long flugelPoints = 0;

	public String getExceed() {
		return exceed;
	}

	public void setExceed(String exceed) {
		this.exceed = exceed;
	}

	public LocalDate getExpiry() {
		return expiry;
	}

	public void setExpiry(LocalDate expiry) {
		this.expiry = expiry;
	}

	public long getImanityPoints() {
		return imanityPoints;
	}

	public void setImanityPoints(long imanityPoints) {
		this.imanityPoints = imanityPoints;
	}

	public long getSeirenPoints() {
		return seirenPoints;
	}

	public void setSeirenPoints(long seirenPoints) {
		this.seirenPoints = seirenPoints;
	}

	public long getWerebeastPoints() {
		return werebeastPoints;
	}

	public void setWerebeastPoints(long werebeastPoints) {
		this.werebeastPoints = werebeastPoints;
	}

	public long getElfPoints() {
		return elfPoints;
	}

	public void setElfPoints(long elfPoints) {
		this.elfPoints = elfPoints;
	}

	public long getExmachinaPoints() {
		return exmachinaPoints;
	}

	public void setExmachinaPoints(long exmachinaPoints) {
		this.exmachinaPoints = exmachinaPoints;
	}

	public long getFlugelPoints() {
		return flugelPoints;
	}

	public void setFlugelPoints(long flugelPoints) {
		this.flugelPoints = flugelPoints;
	}
}
