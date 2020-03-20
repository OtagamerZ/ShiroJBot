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
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "account")
public class Account {
	@Id
	private String userId;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int balance;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT 'Never'")
	private String lastVoted;

	@Column(columnDefinition = "TIMESTAMP DEFAULT NULL")
	private LocalDateTime lastVotedNoFormat;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int streak;

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

	public String getLastVoted() {
		return lastVoted;
	}

	public int voted() {
		this.lastVoted = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3")).format(Helper.dateformat);
		if (lastVotedNoFormat != null && LocalDateTime.now().isBefore(lastVotedNoFormat.plusHours(12))) streak++;
		else streak = 0;

		if (streak > 7) streak = 7;

		lastVotedNoFormat = LocalDateTime.now();

		return streak;
	}

	public int getStreak() {
		return streak;
	}
}
