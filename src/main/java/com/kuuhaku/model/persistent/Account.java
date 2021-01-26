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
import com.kuuhaku.handlers.api.endpoint.DiscordBotsListHandler;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.AnimeName;
import com.kuuhaku.model.enums.CreditLoan;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
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

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int streak = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int bugs = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 1")
	private int stashCapacity = 1;

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

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean voted = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean completedQuests = false;

	@Column(columnDefinition = "TEXT")
	private String buffs = "{}";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT 'Nunca'")
	private String lastVoted = "Nunca";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String ultimate = "";

	@Column(columnDefinition = "CHAR(7) NOT NULL DEFAULT ''")
	private String profileColor = "";

	@Column(columnDefinition = "TEXT")
	private String bg = "https://pm1.narvii.com/6429/7f50ee6d5a42723882c6c23a8420f24dfff60e4f_hq.jpg";

	@Column(columnDefinition = "TEXT")
	private String bio = "";

	@Column(columnDefinition = "TEXT")
	private String dailyProgress = "{}";

	@Temporal(TemporalType.DATE)
	private Calendar lastQuest = null;

	@Enumerated(value = EnumType.STRING)
	private FrameColor frame = FrameColor.PINK;

	@Temporal(TemporalType.DATE)
	private Calendar lastDaily = null;

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
		else if (loan > 0) {
			TransactionDAO.register(userId, from, -credit);
			loan = loan - credit;
		} else {
			TransactionDAO.register(userId, from, credit);
			balance += credit;

			Map<DailyTask, Integer> pg = getDailyProgress();
			pg.compute(DailyTask.CREDIT_TASK, (k, v) -> Helper.getOr(v, 0) + (int) credit);
			setDailyProgress(pg);
		}

		if (loan < 0) {
			balance += loan * -1;
			TransactionDAO.register(userId, from, loan * -1);
			loan = 0;
		}
	}

	public synchronized void addVCredit(long credit, Class<?> from) {
		if (credit == 0) return;
		vBalance += credit;
		TransactionDAO.register(userId, from, credit);
	}

	public synchronized long debitLoan() {
		long stBalance = balance;

		if (balance >= loan) {
			balance -= loan;
			loan = 0;
		} else {
			loan -= balance;
			balance = 0;
		}

		return stBalance - balance;
	}

	public synchronized void removeCredit(long credit, Class<?> from) {
		this.balance -= credit;
		if (credit != 0) TransactionDAO.register(userId, from, -credit);
	}

	public synchronized void consumeCredit(long credit, Class<?> from) {
		long remaining = vBalance - credit;

		if (remaining < 0) {
			this.vBalance = 0;
			this.balance -= Math.abs(remaining);
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
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
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
			voted = true;
			AccountDAO.saveAccount(this);
		}
	}

	public boolean hasVoted() {
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
		try {
			ZonedDateTime lastVote = ZonedDateTime.parse(lastVoted, Helper.dateformat);

			if (today.isBefore(lastVote.plusHours(12)) && voted) {
				return true;
			} else if (today.isBefore(lastVote.plusHours(12))) {
				AtomicReference<Boolean> lock = new AtomicReference<>(null);
				Main.getInfo().getDblApi().hasVoted(userId).thenAccept(voted -> {
					if (voted) {
						DiscordBotsListHandler.retry(userId);
						lock.set(true);
					} else lock.set(false);
				});

				while (lock.get() == null) {
					Thread.sleep(250);
				}

				return lock.get();
			} else return false;
		} catch (DateTimeParseException | InterruptedException ignore) {
			return false;
		}
	}

	public void notifyVote() {
		if (!notified && !lastVoted.equalsIgnoreCase("Nunca")) {
			ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
			ZonedDateTime lastVote = ZonedDateTime.parse(lastVoted, Helper.dateformat);

			if (today.isAfter(lastVote.plusHours(12))) {
				try {
					EmbedBuilder eb = new ColorlessEmbedBuilder();
					eb.setTitle("Opa, você já pode votar novamente!");
					eb.setDescription("Como você pediu, estou aqui para lhe avisar que você já pode [votar novamente](https://top.gg/bot/572413282653306901/vote) para ganhar mais um acúmulo de votos e uma quantia de créditos!");
					eb.setFooter("Data do último voto: " + lastVoted);

					Main.getInfo().getUserByID(userId).openPrivateChannel()
							.flatMap(s -> s.sendMessage(eb.build()))
							.queue(null, e -> Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]));
				} catch (NullPointerException ignore) {
				} finally {
					notified = true;
					AccountDAO.saveAccount(this);
				}
			}
		}
	}

	public int getStreak() {
		try {
			ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
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
			try {
				Kawaipon kp = KawaiponDAO.getKawaipon(userId);

				AnimeName an = AnimeName.valueOf(ultimate);
				if (CardDAO.totalCards(an) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(an) && !k.isFoil()).count())
					return ultimate;
			} catch (IllegalArgumentException e) {
				return "";
			}
		}
		return "";
	}

	public void setUltimate(String ultimate) {
		this.ultimate = ultimate;
	}

	public String getProfileColor() {
		return profileColor;
	}

	public void setProfileColor(String profileColor) {
		this.profileColor = profileColor;
	}

	public String getBg() {
		return bg;
	}

	public void setBackground(String bg) {
		this.bg = bg;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
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

	public int getStashCapacity() {
		return stashCapacity;
	}

	public void setStashCapacity(int stashCapacity) {
		this.stashCapacity = stashCapacity;
	}

	public boolean isVoted() {
		return voted;
	}

	public void setVoted(boolean voted) {
		this.voted = voted;
	}

	public Map<DailyTask, Integer> getDailyProgress() {
		Calendar c = Calendar.getInstance();
		if (dailyProgress == null || lastQuest == null || lastDaily.equals(c)) return new HashMap<>();
		else {
			this.completedQuests = false;
			return new JSONObject(dailyProgress).toMap().entrySet().stream()
					.map(e -> Pair.of(e.getKey(), NumberUtils.toInt(String.valueOf(e.getValue()))))
					.collect(Collectors.toMap(p -> DailyTask.valueOf(p.getLeft()), Pair::getRight));
		}
	}

	public void setDailyProgress(Map<DailyTask, Integer> progress) {
		this.dailyProgress = new JSONObject(progress).toString();
	}

	public boolean hasCompletedQuests() {
		return completedQuests;
	}

	public void setCompletedQuests(boolean completed) {
		this.completedQuests = completed;
	}
}
