/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.LotteryDAO;
import com.kuuhaku.controller.postgresql.TransactionDAO;
import com.kuuhaku.handlers.api.endpoint.DiscordBotsListHandler;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Achievement;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.records.CompletionState;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.apache.commons.collections4.bag.HashBag;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Entity
@Cacheable
@DynamicUpdate
@Table(name = "account")
public class Account {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long balance = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long vBalance = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long sBalance = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long spent = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int gems = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int streak = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int bugs = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 2")
	private int deckStashCapacity = 2;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 50")
	private int cardStashCapacity = 50;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int tutorialStage = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 3")
	private int weeklyRolls = 3;

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

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean dmWarned = false;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String ultimate = "";

	@Column(columnDefinition = "CHAR(7) NOT NULL DEFAULT ''")
	private String profileColor = "";

	@Column(columnDefinition = "TEXT")
	private String bg = "https://pm1.narvii.com/6429/7f50ee6d5a42723882c6c23a8420f24dfff60e4f_hq.jpg";

	@Column(columnDefinition = "TEXT")
	private String bio = "";

	@Column(columnDefinition = "TEXT")
	private String dailyProgress = "{}";

	@Column(columnDefinition = "VARCHAR(255)")
	private String afkMessage = null;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime lastQuest = null;

	@Enumerated(value = EnumType.STRING)
	private FrameColor frame = FrameColor.PINK;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime lastVoted = null;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime lastDaily = null;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime tutorial = null;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("GMT-3"));

	@ElementCollection(fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	@JoinColumn(nullable = false, name = "account_id")
	private Set<Achievement> achievements = EnumSet.noneOf(Achievement.class);

	private transient Map<String, CompletionState> compState = null;
	private transient FrameColor cachedFrame = null;
	private transient Card ultimateCard = null;

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public long getBalance() {
		return balance;
	}

	public long getVBalance() {
		return vBalance + sBalance;
	}

	public long getTotalBalance() {
		return balance + vBalance + sBalance;
	}

	public long getSpent() {
		return spent;
	}

	public void addCredit(long credit, Class<?> from) {
		if (credit == 0) return;

		TransactionDAO.register(uid, from, credit);
		balance += credit;

		if (hasPendingQuest()) {
			Map<DailyTask, Integer> pg = getDailyProgress();
			pg.merge(DailyTask.CREDIT_TASK, (int) credit, Integer::sum);
			setDailyProgress(pg);
		}
	}

	public void addVCredit(long credit, Class<?> from) {
		if (credit == 0) return;
		vBalance += credit;
		TransactionDAO.register(uid, from, credit);
	}

	public void addSCredit(long credit, Class<?> from) {
		if (credit == 0) return;
		sBalance += credit;
		TransactionDAO.register(uid, from, credit);
	}

	public void setSBalance(long sBalance) {
		this.sBalance = sBalance;
	}

	public void removeCredit(long credit, Class<?> from) {
		this.spent += credit;
		this.balance -= credit;

		if (credit != 0) TransactionDAO.register(uid, from, -credit);
	}

	public void consumeCredit(long credit, Class<?> from) {
		if (credit != 0) TransactionDAO.register(uid, from, -credit);
		spent += credit;

		long aux = credit;
		credit -= sBalance;
		sBalance = Math.max(0, sBalance - aux);

		if (credit > 0) {
			aux = credit;
			credit -= vBalance;
			vBalance = Math.max(0, vBalance - aux);

			if (credit > 0) {
				balance -= credit;
			}
		}

		LotteryValue lv = LotteryDAO.getLotteryValue();
		lv.addValue(Math.round(credit * 0.1));
		LotteryDAO.saveLotteryValue(lv);
	}

	public void expireVCredit() {
		this.vBalance *= 0.75;
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

	public int getTutorialStage() {
		return tutorialStage;
	}

	public void setTutorialStage(int tutorialStage) {
		this.tutorialStage = tutorialStage;
	}

	public void completeTutorial() {
		this.tutorial = ZonedDateTime.now(ZoneId.of("GMT-3"));
	}

	public boolean hasCompletedTutorial() {
		return tutorial != null;
	}

	public int getWeeklyRolls() {
		return weeklyRolls;
	}

	public void setWeeklyRolls(int weeklyRolls) {
		this.weeklyRolls = weeklyRolls;
	}

	public boolean hasNoviceDeck() {
		if (this.tutorial == null) return false;
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));

		return today.isBefore(tutorial.plusMonths(1));
	}

	public ZonedDateTime getCreationDate() {
		return createdAt;
	}

	public boolean isOldPlayer() {
		return createdAt.isBefore(ZonedDateTime.of(LocalDateTime.of(2022, 2, 12, 0, 0), ZoneId.of("GMT-3")));
	}

	public void voted() {
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
		if (lastVoted == null) streak = 1;
		else try {
			Helper.logger(this.getClass()).info("""
																	
							Voto anterior: %s
							Hoje: %s
							Acumula? %s
									""".formatted(
							lastVoted.format(Helper.FULL_DATE_FORMAT),
							today.format(Helper.FULL_DATE_FORMAT),
							today.isBefore(lastVoted.plusHours(24))
					)
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
						CompletableFuture<Boolean> voteCheck = CompletableFuture.supplyAsync(() ->
								Helper.get("https://top.gg/api/bots/572413282653306901/check",
										new JSONObject() {{
											put("userId", uid);
										}}
								, ShiroInfo.getDblToken())
						).thenApply(payload -> {
							boolean voted = payload.getInt("voted") == 1;
							if (voted) {
								DiscordBotsListHandler.retry(uid);
							}

							return voted;
						});

						return voteCheck.get(1, TimeUnit.MINUTES);
					}

					return false;
				}
			} catch (DateTimeParseException | NullPointerException e) {
				if (thenApply) {
					CompletableFuture<Boolean> voteCheck = CompletableFuture.supplyAsync(() ->
							Helper.get("https://top.gg/api/bots/572413282653306901/check",
									new JSONObject() {{
										put("userId", uid);
									}}
							, ShiroInfo.getDblToken())
					).thenApply(payload -> {
						boolean voted = payload.getInt("voted") == 1;
						if (voted) {
							DiscordBotsListHandler.retry(uid);
						}

						return voted;
					});

					return voteCheck.get(1, TimeUnit.MINUTES);
				}

				return false;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
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
					eb.setDescription("Como você pediu, estou aqui para lhe avisar que você já pode [votar novamente](https://top.gg/bot/572413282653306901/vote) para ganhar mais um acúmulo de votos e uma quantia de CR!");
					eb.setFooter("Data do último voto: " + lastVoted);

					Main.getInfo().getUserByID(uid).openPrivateChannel()
							.flatMap(s -> s.sendMessageEmbeds(eb.build()))
							.queue(null, e -> remind = false);
				} catch (NullPointerException ignore) {
				} finally {
					notified = true;
					AccountDAO.saveAccount(this);
				}
			}
		}
	}

	public Set<Achievement> getAchievements() {
		return achievements;
	}

	public HashBag<Achievement.Medal> getMedalBag() {
		return achievements.stream()
				.map(Achievement::getMedal)
				.collect(Collectors.toCollection(HashBag::new));
	}

	public boolean isUsingFoil() {
		return useFoil;
	}

	public void setUseFoil(boolean useFoil) {
		this.useFoil = useFoil;
	}

	public boolean isDMWarned() {
		return dmWarned;
	}

	public void setDMWarned(boolean dmWarned) {
		this.dmWarned = dmWarned;
	}

	public int getStreak() {
		try {
			ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));

			if (lastVoted == null || !today.isBefore(lastVoted.plusHours(24))) streak = 0;
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
		this.gems = Math.max(0, this.gems - 1);
	}

	public void removeGem(int qtd) {
		gems = Math.max(0, this.gems - qtd);
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

	public FrameColor getFrame() {
		if (cachedFrame == null) {
			cachedFrame = frame.canUse(this) ? frame : FrameColor.PINK;
		}

		return cachedFrame;
	}

	public void setFrame(FrameColor frame) {
		this.cachedFrame = frame;
		this.frame = frame;
	}

	public Card getUltimate() {
		if (ultimate != null && !ultimate.isBlank()) {
			try {
				AddedAnime an = CardDAO.verifyAnime(ultimate);
				if (getCompletion(an).any()) {
					if (ultimateCard == null)
						ultimateCard = CardDAO.getUltimate(ultimate);

					return ultimateCard;
				}
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		return null;
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

	public int getDeckStashCapacity() {
		return deckStashCapacity;
	}

	public void setDeckStashCapacity(int deckStashCapacity) {
		this.deckStashCapacity = deckStashCapacity;
	}

	public int getCardStashCapacity() {
		return cardStashCapacity;
	}

	public void setCardStashCapacity(int cardStashCapacity) {
		this.cardStashCapacity = cardStashCapacity;
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

		Map<DailyTask, Integer> tasks = new HashMap<>();
		JSONObject prog = new JSONObject(dailyProgress);
		int date = prog.getInt("DATE", -1);
		for (String k : prog.keySet()) {
			try {
				tasks.put(DailyTask.valueOf(k), prog.getInt(k));
			} catch (IllegalArgumentException ignore) {
			}
		}

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

	public boolean isAfk() {
		return afkMessage != null;
	}

	public String getAfkMessage() {
		return afkMessage;
	}

	public void setAfkMessage(String afkMessage) {
		this.afkMessage = afkMessage;
	}

	public Map<String, CompletionState> getCompState() {
		if (compState == null)
			compState = CardDAO.getCompletionState(uid);

		return compState;
	}

	public CompletionState getCompletion(String anime) {
		return getCompState().getOrDefault(anime, new CompletionState(false, false));
	}

	public CompletionState getCompletion(AddedAnime anime) {
		return getCompState().getOrDefault(anime.getName(), new CompletionState(false, false));
	}
}
