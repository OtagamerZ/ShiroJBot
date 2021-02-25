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

import com.kuuhaku.controller.postgresql.TransactionDAO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "supportrating")
public class SupportRating {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String id;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int thanksTokens = 0;

	public SupportRating(String id) {
		this.id = id;
	}

	public SupportRating() {
	}

	public String getId() {
		return id;
	}

	public int getThanksTokens() {
		return thanksTokens;
	}

	public void addThanksToken(String from) {
		TransactionDAO.register(id, from, 1);
		thanksTokens += 1;
	}

	public void addThanksToken(String from, int amount) {
		TransactionDAO.register(id, from, amount);
		thanksTokens += amount;
	}

	public void useThanksToken() {
		TransactionDAO.register(id, id, -1);
		thanksTokens = Math.max(thanksTokens - 1, 0);
	}

	public void useThanksToken(int amount) {
		TransactionDAO.register(id, id, -amount);
		thanksTokens = Math.max(thanksTokens - amount, 0);
	}
}
