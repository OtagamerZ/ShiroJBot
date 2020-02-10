/*
 * This file is part of Shiro J Bot.
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
import java.time.LocalDateTime;

@Entity
public class Account {
	@Id
	private String userId;

	@Column(columnDefinition = "INT DEFAULT 0")
	private int balance;

	@Column(columnDefinition = "VARCHAR(191) DEFAULT 'never'")
	private String lastVoted;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public int getBalance() {
		return balance;
	}

	public void addCredit(int credit) {
		this.balance += credit;
	}

	public void removeCredit(int credit) {
		this.balance -= credit;
	}

	public void voted() {
		this.lastVoted = LocalDateTime.now().format(Helper.dateformat);
	}
}
