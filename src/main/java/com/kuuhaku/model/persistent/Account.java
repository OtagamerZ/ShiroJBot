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
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.AnimeName;
import com.kuuhaku.model.enums.CreditLoan;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Entity
@Table(name = "account")
public class Account {
	@Id
	private String userId;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String twitchId = "";

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long balance = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long vBalance = 0;

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

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean follower = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
	private boolean receiveNotifs = true;

	@Column(columnDefinition = "TEXT")
	private String buffs = "{}";

	@Enumerated(value = EnumType.STRING)
	private FrameColor frame = FrameColor.PINK;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String ultimate = "";

	@Temporal(TemporalType.DATE)
	private Calendar lastDaily = null;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int bugs = 0;

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

	public long getVBalance() {
		return vBalance;
	}

	public long getTotalBalance() {
		return balance + vBalance;
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

	public synchronized void addVCredit(long credit, Class<?> from) {
		if (credit == 0) return;
		else if (this.loan > 0) {
			TransactionDAO.register(userId, from, -credit);
			loan = loan - credit;
		} else {
			TransactionDAO.register(userId, from, credit);
			vBalance += credit;
		}

		if (loan < 0) {
			vBalance += loan * -1;
			TransactionDAO.register(userId, from, loan * -1);
			loan = 0;
		}
	}

	public synchronized void removeCredit(long credit, Class<?> from) {
		this.balance -= credit;
		if (credit != 0) TransactionDAO.register(userId, from, -credit);
	}

	public synchronized void consumeCredit(long credit, Class<?> from) {
		long remaining = vBalance - credit;

		if (remaining > 0) {
			this.vBalance = 0;
			this.balance -= remaining;
		} else {
			this.vBalance -= credit;
		}

		if (credit != 0) TransactionDAO.register(userId, from, -credit);
	}

	public synchronized void expireVCredit() {
		this.vBalance *= 0.75;
	}

	public synchronized void addLoan(long loan) {
		this.loan += loan;
		AccountDAO.saveAccount(this);
	}

	public String getLastVoted() {
		return lastVoted;
	}

	public Calendar getLastDaily() {
		return lastDaily;
	}

	public boolean hasDailyAvailable() {
		if (this.lastDaily == null) return true;
		Calendar today = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT-3")));
		Calendar lastDaily = this.lastDaily;

		return today.get(Calendar.DAY_OF_YEAR) > lastDaily.get(Calendar.DAY_OF_YEAR);
	}

	public void playedDaily() {
		this.lastDaily = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT-3")));
	}

	public void voted() {
		ZonedDateTime today = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"));
		try {
			ZonedDateTime lastVote = ZonedDateTime.parse(lastVoted, Helper.dateformat);

			Helper.logger(this.getClass()).info("""		
     										
					Voto anterior: %s							       
					Hoje: %s     
					Acumula? %s
					""".formatted(lastVote.format(Helper.dateformat), today.format(Helper.dateformat), today.isBefore(lastVote.plusHours(24)))
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
		if (!notified && !lastVoted.equalsIgnoreCase("Nunca")) {
			ZonedDateTime today = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"));
			ZonedDateTime lastVote = ZonedDateTime.parse(lastVoted, Helper.dateformat);

			if (today.isAfter(lastVote.plusHours(12))) {
				try {
					Main.getInfo().getUserByID(userId).openPrivateChannel().queue(c -> {
								EmbedBuilder eb = new ColorlessEmbedBuilder();
								eb.setTitle("Opa, você já pode votar novamente!");
								eb.setDescription("Como você pediu, estou aqui para lhe avisar que você já pode [votar novamente](https://top.gg/bot/572413282653306901/vote) para ganhar mais um acúmulo de votos e uma quantia de créditos!");
								eb.setFooter("Data do último voto: " + lastVoted);
								c.sendMessage(eb.build()).queue(null, e -> Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]));
							}, e -> Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0])
					);
				} catch (NullPointerException e) {
					Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
				} finally {
					notified = true;
					AccountDAO.saveAccount(this);
				}
			}
		}
	}

	public int getStreak() {
		try {
			ZonedDateTime today = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3"));
			ZonedDateTime lastVote = ZonedDateTime.parse(lastVoted, Helper.dateformat);

			if (today.isAfter(lastVote.plusHours(24))) streak = 0;
		} catch (DateTimeParseException ignore) {
		}

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

	public boolean isFollower() {
		return follower;
	}

	public void setFollower(boolean follower) {
		this.follower = follower;
	}

	public boolean isReceivingNotifs() {
		return receiveNotifs;
	}

	public void setReceiveNotifs(boolean receiveNotifs) {
		this.receiveNotifs = receiveNotifs;
	}

	public Map<String, Integer> getBuffs() {
		if (buffs == null) return new HashMap<>();
		else return new JSONObject(buffs)
				.toMap()
				.entrySet()
				.stream()
				.map(e -> Pair.of(e.getKey(), (int) e.getValue()))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
	}

	public void setBuffs(Map<String, Integer> buffs) {
		this.buffs = new JSONObject(buffs).toString();
	}

	public void addBuff(String id) {
		Map<String, Integer> buffs = getBuffs();
		buffs.put(id, buffs.getOrDefault(id, 0) + 1);
		setBuffs(buffs);
	}

	public void removeBuff(String id) {
		Map<String, Integer> buffs = getBuffs();
		buffs.put(id, Math.max(0, buffs.getOrDefault(id, 0) - 1));
		setBuffs(buffs);
	}

	public FrameColor getFrame() {
		return frame;
	}

	public void setFrame(FrameColor frame) {
		this.frame = frame;
	}

	public String getUltimate() {
		if (ultimate != null && !ultimate.isBlank()) {
			Kawaipon kp = KawaiponDAO.getKawaipon(userId);

			AnimeName an = AnimeName.valueOf(ultimate);
			if (CardDAO.totalCards(an) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(an) && !k.isFoil()).count())
				return ultimate;
		}
		return "";
	}

	public void setUltimate(String ultimate) {
		this.ultimate = ultimate;
	}

	public int getBugs() {
		return bugs;
	}

	public void addBug() {
		this.bugs++;
	}

	public void setBugs(int bugs) {
		this.bugs = bugs;
	}
}
