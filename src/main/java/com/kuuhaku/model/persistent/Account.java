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

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.TransactionDAO;
import com.kuuhaku.utils.CreditLoan;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;

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

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String twitchId;

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

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean remind = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean notified = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean animatedBg = false;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTwitchId() {
		return twitchId;
	}

	public void setTwitchId(String twitchId) {
		this.twitchId = twitchId;
	}

	public long getBalance() {
		return balance;
	}

	public long getLoan() {
		return loan;
	}

	public void signLoan(CreditLoan loan) {
		ExceedMember ex = ExceedDAO.getExceedMember(userId);
		this.addCredit(loan.getLoan(), this.getClass());
		this.loan = Math.round(loan.getLoan() * loan.getInterest(ex));
	}

	public synchronized void addCredit(long credit, Class<?> from) {
		if (credit == 0) return;
		else if (this.loan > 0) {
			TransactionDAO.register(userId, from, -credit);
			loan = loan - credit;
		} else {
			TransactionDAO.register(userId, from, credit);
			balance += credit;
		}

		if (loan < 0) {
			balance += loan * -1;
			TransactionDAO.register(userId, from, loan * -1);
			loan = 0;
		}
	}

	public synchronized void removeCredit(long credit, Class<?> from) {
		this.balance -= credit;
		if (credit != 0) TransactionDAO.register(userId, from, -credit);
	}

	public String getLastVoted() {
		return lastVoted;
	}

	public void voted() {
		ZonedDateTime today = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"));
		try {
			ZonedDateTime lastVote = ZonedDateTime.parse(lastVoted, Helper.dateformat);

			Helper.logger(this.getClass()).info(
					"\nVoto anterior: " + lastVote.format(Helper.dateformat) +
							"\nHoje: " + today.format(Helper.dateformat) +
							"\nAcumula? " + today.isBefore(lastVote.plusHours(24))
			);

			if (today.isBefore(lastVote.plusHours(24)) || streak == 0) streak = Helper.clamp(streak + 1, 0, 7);
			else streak = 0;
		} catch (DateTimeParseException ignore) {
		} finally {
			lastVoted = today.format(Helper.dateformat);
			notified = false;
			AccountDAO.saveAccount(this);
		}
	}

	public void notifyVote() {
		if (!notified) {
			ZonedDateTime today = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"));
			ZonedDateTime lastVote = ZonedDateTime.parse(lastVoted, Helper.dateformat);

			if (today.isAfter(lastVote.plusHours(12))) {
				try {
					Main.getInfo().getUserByID(userId).openPrivateChannel().queue(c -> {
						EmbedBuilder eb = new EmbedBuilder();
						eb.setColor(Helper.getRandomColor());
						eb.setTitle("Opa, você já pode votar novamente!");
						eb.setDescription("Como você pediu, estou aqui para lhe avisar que você já pode [votar novamente](https://top.gg/bot/572413282653306901/vote) para ganhar mais um acúmulo de votos e uma quantia de créditos!");
						eb.setFooter("Data do último voto: " + lastVoted);
						c.sendMessage(eb.build()).queue(null, Helper::doNothing);
					}, Helper::doNothing);
				} catch (NullPointerException ignore) {
				} finally {
					notified = true;
					AccountDAO.saveAccount(this);
				}
			}
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

	public boolean shouldRemind() {
		return remind;
	}

	public void setRemind(boolean remind) {
		this.remind = remind;
	}

	public boolean hasAnimatedBg() {
		return animatedBg;
	}

	public void setAnimatedBg(boolean animatedBg) {
		this.animatedBg = animatedBg;
	}
}
