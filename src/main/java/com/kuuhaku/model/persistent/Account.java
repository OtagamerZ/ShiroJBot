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

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.utils.CreditLoan;
import com.kuuhaku.utils.Helper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

@Entity
@Table(name = "account")
public class Account {
	@Id
	private String userId;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long balance = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long loan = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int gems = 0;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT 'Nunca'")
	private String lastVoted = "Nunca";

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int streak = 0;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public long getBalance() {
		return balance;
	}

	public long getLoan() {
		return loan;
	}

	public void signLoan(CreditLoan loan) {
		ExceedMember ex = ExceedDAO.getExceedMember(userId);
		this.addCredit(loan.getLoan());
		this.loan = Math.round(loan.getLoan() * loan.getInterest(ex));
	}

	public void addCredit(long credit) {
		if (this.loan > 0) loan = Helper.clamp(loan - credit, 0, loan);
		else balance += credit;
	}

	public void removeCredit(long credit) {
		this.balance -= credit;
	}

	public String getLastVoted() {
		return lastVoted;
	}

	public void voted() {
		ZonedDateTime today = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"));
		try {
			ZonedDateTime lastVote = ZonedDateTime.parse(lastVoted, Helper.dateformat);

			if (today.isBefore(lastVote.plusHours(24))) streak = Helper.clamp(streak + 1, 0, 7);
			else streak = 0;
		} catch (DateTimeParseException e) {
			lastVoted = today.format(Helper.dateformat);
		} finally {
			AccountDAO.saveAccount(this);
		}
	}

	public int getStreak() {
		return streak;
	}

	public void setStreak(int streak) {
		this.streak = streak;
	}

	public int getGems() {
		return gems;
	}

	public void addGem() {
		this.gems++;
	}

	public void addGem(int qtd) {
		gems += qtd;
	}

	public void removeGem() {
		this.gems--;
	}

	public void removeGem(int qtd) {
		gems -= qtd;
	}
}
