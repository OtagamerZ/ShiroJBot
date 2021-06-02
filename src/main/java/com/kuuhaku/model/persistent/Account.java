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

import com.google.gson.JsonElement;
import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.handlers.api.endpoint.DiscordBotsListHandler;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.CreditLoan;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Entity
@DynamicUpdate
@Table(name = "account")
public class Account {
	@Id
	private String uid;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String twitchId = "";

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long balance = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long vBalance = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long loan = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long stocksProfit = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long spent = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int gems = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int streak = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int bugs = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 2")
	private int stashCapacity = 2;

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
	private boolean useFoil = false;

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

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime lastQuest = null;

	@Enumerated(value = EnumType.STRING)
	private FrameColor frame = FrameColor.PINK;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime lastVoted = null;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime lastDaily = null;

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
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

	public long getSpent() {
		return spent;
	}

	public long getLoan() {
		return loan;
	}

	public void signLoan(CreditLoan loan) {
		ExceedMember ex = ExceedDAO.getExceedMember(uid);
		this.addCredit(loan.getLoan(), this.getClass());
		this.loan = Math.round(loan.getLoan() * loan.getInterest(ex));
	}

	public void addCredit(long credit, Class<?> from) {
		if (credit == 0) return;
		else if (loan > 0) {
			TransactionDAO.register(uid, from, -credit);
			loan = loan - credit;
		} else {
			TransactionDAO.register(uid, from, credit);
			balance += credit;

			if (hasPendingQuest()) {
				Map<DailyTask, Integer> pg = getDailyProgress();
				pg.merge(DailyTask.CREDIT_TASK, (int) credit, Integer::sum);
				setDailyProgress(pg);
			}
		}

		if (loan < 0) {
			balance += loan * -1;
			TransactionDAO.register(uid, from, loan * -1);
			loan = 0;
		}
	}

	public void addVCredit(long credit, Class<?> from) {
		if (credit == 0) return;
		vBalance += credit;
		TransactionDAO.register(uid, from, credit);
	}

	public long debitLoan() {
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

	public void removeCredit(long credit, Class<?> from) {
		this.balance -= credit;
		this.spent += credit;
		if (credit != 0) TransactionDAO.register(uid, from, -credit);
	}

	public void consumeCredit(long credit, Class<?> from) {
		long remaining = vBalance - credit;

		if (remaining < 0) {
			this.vBalance = 0;
			this.balance -= Math.abs(remaining);
			this.spent += Math.abs(remaining);
		} else {
			this.vBalance -= credit;
		}

		if (credit != 0) TransactionDAO.register(uid, from, -credit);
	}

	public void expireVCredit() {
		this.vBalance *= 0.75;
	}

	public void addLoan(long loan) {
		this.loan += loan;
		AccountDAO.saveAccount(this);
	}

	public long getStocksProfit() {
		return stocksProfit;
	}

	public void addProfit(long value) {
		this.stocksProfit += value;
	}

	public void removeProfit(long value) {
		this.stocksProfit -= value;
	}

	public ZonedDateTime getLastVoted() {
		return lastVoted;
	}

	public ZonedDateTime getLastDaily() {
		return lastDaily;
	}

	public boolean hasDailyAvailable() {
		if (this.lastDaily == null) return true;
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));

		return today.getDayOfYear() > lastDaily.getDayOfYear();
	}

	public void playedDaily() {
		this.lastDaily = ZonedDateTime.now(ZoneId.of("GMT-3"));
	}

	public void voted() {
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
		if (lastVoted == null) streak = 1;
		else try {
			Helper.logger(this.getClass()).info("""
															
					Voto anterior: %s
					Hoje: %s
					Acumula? %s
					""".formatted(Helper.fullDateFormat.format(lastVoted), today.format(Helper.fullDateFormat), today.isBefore(lastVoted.plusHours(24)))
			);

			if (today.isBefore(lastVoted.plusHours(24)) || streak == 0) streak = Helper.clamp(streak + 1, 0, 7);
			else streak = 0;
		} catch (DateTimeParseException ignore) {
		}

		lastVoted = today;
		notified = false;
		voted = true;
		AccountDAO.saveAccount(this);
	}

	public boolean hasVoted(boolean thenApply) {
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
		try {
			try {
				if (today.isBefore(lastVoted.plusHours(12)) && voted) {
					return true;
				} else {
					if (thenApply) {
						CompletableFuture<Boolean> voteCheck = new CompletableFuture<>();
						Main.getInfo().getDblApi().hasVoted(uid).thenAccept(voted -> {
							if (voted) {
								DiscordBotsListHandler.retry(uid);
							}

							voteCheck.complete(voted);
						});

						return voteCheck.get();
					}

					return false;
				}
			} catch (DateTimeParseException | NullPointerException e) {
				if (thenApply) {
					CompletableFuture<Boolean> voteCheck = new CompletableFuture<>();
					Main.getInfo().getDblApi().hasVoted(uid).thenAccept(voted -> {
						if (voted) {
							DiscordBotsListHandler.retry(uid);
						}
						voteCheck.complete(voted);
					});

					return voteCheck.get();
				}

				return false;
			}
		} catch (InterruptedException | ExecutionException e) {
			return false;
		}
	}

	public void notifyVote() {
		if (!notified && lastVoted != null) {
			ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));

			if (today.isAfter(lastVoted.plusHours(12))) {
				try {
					EmbedBuilder eb = new ColorlessEmbedBuilder();
					eb.setTitle("Opa, você já pode votar novamente!");
					eb.setDescription("Como você pediu, estou aqui para lhe avisar que você já pode [votar novamente](https://top.gg/bot/572413282653306901/vote) para ganhar mais um acúmulo de votos e uma quantia de créditos!");
					eb.setFooter("Data do último voto: " + lastVoted);

					Main.getInfo().getUserByID(uid).openPrivateChannel()
							.flatMap(s -> s.sendMessage(eb.build()))
							.queue(null, e -> remind = false);
				} catch (NullPointerException ignore) {
				} finally {
					notified = true;
					AccountDAO.saveAccount(this);
				}
			}
		}
	}

	public boolean isUsingFoil() {
		return useFoil;
	}

	public void setUseFoil(boolean useFoil) {
		this.useFoil = useFoil;
	}

	public int getStreak() {
		try {
			ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));

			if (lastVoted != null && today.isAfter(lastVoted.plusHours(24))) streak = 0;
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

	public FrameColor getFrame() {
		return frame;
	}

	public void setFrame(FrameColor frame) {
		this.frame = frame;
	}

	public String getUltimate() {
		if (ultimate != null && !ultimate.isBlank()) {
			try {
				Kawaipon kp = KawaiponDAO.getKawaipon(uid);

				AddedAnime an = CardDAO.verifyAnime(ultimate);
				if (CardDAO.totalCards(an.getName()) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(an) && !k.isFoil()).count())
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
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));

		if (dailyProgress == null) {
			return new HashMap<>();
		} else if (lastQuest == null) {
			ZonedDateTime prev = ZonedDateTime.now(ZoneId.of("GMT-3"));
			prev.minusDays(1);
			lastQuest = prev;
		}

		JSONObject prog = new JSONObject(dailyProgress);
		JsonElement je = prog.remove("DATE");
		int date = je == null ? -1 : je.getAsInt();
		Map<DailyTask, Integer> tasks = prog.toMap().entrySet().stream()
				.filter(e -> Arrays.stream(DailyTask.values()).anyMatch(dt -> dt.name().equals(e.getKey())))
				.map(e -> Pair.of(DailyTask.valueOf(e.getKey()), NumberUtils.toInt(String.valueOf(e.getValue()))))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

		if (date > -1 && (date != today.getDayOfYear() || lastQuest.getDayOfYear() == today.getDayOfYear())) {
			setDailyProgress(new HashMap<>());
			return new HashMap<>();
		} else {
			return tasks;
		}
	}

	public void setDailyProgress(Map<DailyTask, Integer> progress) {
		Calendar c = Calendar.getInstance();
		JSONObject prog = new JSONObject(progress);
		prog.put("DATE", c.get(Calendar.DAY_OF_YEAR));
		this.dailyProgress = prog.toString();
	}

	public void setLastQuest() {
		this.lastQuest = ZonedDateTime.now(ZoneId.of("GMT-3"));
	}

	public boolean hasPendingQuest() {
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
		return lastQuest == null || lastQuest.getDayOfYear() != today.getDayOfYear();
	}
}
