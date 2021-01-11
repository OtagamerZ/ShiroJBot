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
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "supportrating")
public class SupportRating {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String id;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private float interaction = 0;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private float solution = 0;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private float knowledge = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int monthlyTickets = 0;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT 'Nunca'")
	private String lastTicket = "Nunca";

	public SupportRating(String id) {
		this.id = id;
	}

	public SupportRating() {
	}

	public String getId() {
		return id;
	}

	public float getInteraction() {
		return interaction;
	}

	public void setInteraction(float interaction) {
		this.interaction = interaction;
	}

	public float getSolution() {
		return solution;
	}

	public void setSolution(float solution) {
		this.solution = solution;
	}

	public float getKnowledge() {
		return knowledge;
	}

	public void setKnowledge(float knowledge) {
		this.knowledge = knowledge;
	}

	public String getLastTicket() {
		return lastTicket;
	}

	public int getMonthlyTickets() {
		return monthlyTickets;
	}

	public void addTicket() {
		this.monthlyTickets++;
		this.lastTicket = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3")).format(Helper.dateformat);
	}

	public void resetTickets() {
		this.monthlyTickets = 0;
	}
}
