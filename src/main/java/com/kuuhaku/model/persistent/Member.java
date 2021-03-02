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
import com.kuuhaku.handlers.api.endpoint.payload.Bonus;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.enums.TrophyType;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.DynamicUpdate;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Entity
@DynamicUpdate
@Table(name = "member")
public class Member {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String uid = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String sid = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String pseudoName = "";

	@Column(columnDefinition = "TEXT")
	private String pseudoAvatar = "";

	//NUMBERS
	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int level = 1;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long xp = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long lastVoted = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long lastEarntXp = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long voiceTime = 0;

	//SWITCHES
	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean markForDelete = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean rulesSent = false;

	@Enumerated(value = EnumType.STRING)
	private TrophyType trophy = null;

	public Member() {

	}

	public static List<Bonus> getBonuses(User u) {
		List<Bonus> bonuses = new ArrayList<>();

		if (ExceedDAO.hasExceed(u.getId()) && Main.getInfo().getWinner().equals(ExceedDAO.getExceed(u.getId())))
			bonuses.add(new Bonus(0, "Exceed Vitorioso", 2));
		if (!getWaifu(u.getId()).isEmpty())
			bonuses.add(new Bonus(1, "Waifu", WaifuDAO.getMultiplier(u).getMult()));

		Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
		if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 1)
			bonuses.add(new Bonus(4, "Coleção de cartas (100%)", 2));
		else if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 0.75)
			bonuses.add(new Bonus(4, "Coleção de cartas (75%)", 1.75f));
		else if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 0.5)
			bonuses.add(new Bonus(4, "Coleção de cartas (50%)", 1.5f));
		else if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 0.25)
			bonuses.add(new Bonus(4, "Coleção de cartas (25%)", 1.25f));

		return bonuses;
	}

	public static String getWaifu(String id) {
		Couple c = WaifuDAO.getCouple(id);
		if (c == null) return "";
		return c.getHusbando().equals(id) ? c.getWaifu() : c.getHusbando();
	}

	public synchronized boolean addXp(Guild g) {
		User u = Main.getInfo().getUserByID(uid);
		AtomicReference<Double> mult = new AtomicReference<>(1d);

		if (ExceedDAO.hasExceed(uid) && Main.getInfo().getWinner().equals(ExceedDAO.getExceed(uid)))
			mult.updateAndGet(v -> v * 2);
		if (g.getMembers().stream().map(net.dv8tion.jda.api.entities.Member::getId).collect(Collectors.toList()).contains(Member.getWaifu(u.getId())))
			mult.updateAndGet(v -> v * WaifuDAO.getMultiplier(u).getMult());

		Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
		if (kp.getCards().size() / ((float) CardDAO.totalCards()) * 2 >= 1) {
			mult.updateAndGet(v -> v * 1.5f);
		} else if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 0.75)
			mult.updateAndGet(v -> v * 1.37f);
		else if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 0.5)
			mult.updateAndGet(v -> v * 1.25f);
		else if (kp.getCards().size() / ((float) CardDAO.totalCards() * 2) >= 0.25)
			mult.updateAndGet(v -> v * 1.12f);

		GuildBuff gb = GuildBuffDAO.getBuffs(g.getId());
		gb.getBuffs().stream().filter(b -> b.getId() == 1).findAny().ifPresent(b -> mult.updateAndGet(v -> v * b.getMult()));

		float spamModif = Math.max(0, Math.min((System.currentTimeMillis() - lastEarntXp) / 1000f, 1));
		xp += 15 * mult.get() * spamModif;
		lastEarntXp = System.currentTimeMillis();

		Account acc = AccountDAO.getAccount(uid);
		if (acc.hasPendingQuest()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.compute(DailyTask.XP_TASK, (k, v) -> Helper.getOr(v, 0) + (int) Math.round(15 * mult.get() * spamModif));
			acc.setDailyProgress(pg);
			AccountDAO.saveAccount(acc);
		}

		ExceedMember em = ExceedDAO.getExceedMember(uid);
		if (em != null) {
			em.addContribution((int) (15 * mult.get() * spamModif));
			ExceedDAO.saveExceedMember(em);
		}

		if (xp >= (long) Math.pow(level, 2) * 100) {
			level++;
			acc.addCredit(75 + (8L * level), this.getClass());
			AccountDAO.saveAccount(acc);
			return true;
		}
		return false;
	}

	public synchronized long addXp(int amount) {
		float spamModif = Math.max(0, Math.min((System.currentTimeMillis() - lastEarntXp) / 10000f, 1));
		xp += amount * spamModif;
		lastEarntXp = System.currentTimeMillis();

		ExceedMember em = ExceedDAO.getExceedMember(uid);
		if (em != null) {
			em.addContribution((int) (amount * spamModif));
			ExceedDAO.saveExceedMember(em);
		}

		Account acc = AccountDAO.getAccount(uid);
		if (acc.hasPendingQuest()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.compute(DailyTask.XP_TASK, (k, v) -> Helper.getOr(v, 0) + Math.round(amount * spamModif));
			acc.setDailyProgress(pg);
			AccountDAO.saveAccount(acc);
		}

		if (xp >= (long) Math.pow(level, 2) * 100) {
			level++;
			acc.addCredit(75 + (8L * level), this.getClass());
			AccountDAO.saveAccount(acc);
		}
		return (long) (amount * spamModif);
	}

	public void recalculateLevel() {
		level = (int) Math.ceil(Math.sqrt(xp / 100f));
	}

	public void halfXp() {
		xp /= 2;
		recalculateLevel();
	}

	public void halfXpKeepLevel() {
		xp /= 2;
	}

	public void resetXp() {
		level = 1;
		xp = 0;
	}

	public long getVoiceTime() {
		return voiceTime;
	}

	public void setVoiceTime(long voiceTime) {
		this.voiceTime += voiceTime;
	}

	public String getId() {
		return id;
	}

	public int getLevel() {
		return level;
	}

	public long getXp() {
		return xp;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isMarkForDelete() {
		return markForDelete;
	}

	public void setMarkForDelete(boolean markForDelete) {
		this.markForDelete = markForDelete;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String mid) {
		this.uid = mid;
	}

	public boolean isRulesSent() {
		return rulesSent;
	}

	public void setRulesSent(boolean rulesSent) {
		this.rulesSent = rulesSent;
	}

	public boolean canVote() {
		return (System.currentTimeMillis() / 1000) - lastVoted > 86400;
	}

	public void vote() {
		lastVoted = System.currentTimeMillis() / 1000;
	}

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getPseudoName() {
		return pseudoName;
	}

	public void setPseudoName(String pseudoName) {
		this.pseudoName = pseudoName;
	}

	public String getPseudoAvatar() {
		return pseudoAvatar;
	}

	public void setPseudoAvatar(String pseudoAvatar) {
		this.pseudoAvatar = pseudoAvatar;
	}

	public TrophyType getTrophy() {
		return trophy;
	}

	public void setTrophy(TrophyType trophy) {
		this.trophy = trophy;
	}

	public JSONObject toJson() {
		Account acc = AccountDAO.getAccount(uid);

		return new JSONObject() {{
			put("id", id);
			put("mid", uid);
			put("sid", sid);
			put("pseudoName", pseudoName);
			put("profileColor", acc.getProfileColor());
			put("bg", acc.getBg());
			put("bio", acc.getBio());
			put("pseudoAvatar", pseudoAvatar);
			put("level", level);
			put("xp", xp);
			put("lastVoted", lastVoted);
			put("markForDelete", markForDelete);
			put("rulesSent", rulesSent);
		}};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Member member = (Member) o;
		return Objects.equals(id, member.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
