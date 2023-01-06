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

import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.handlers.api.endpoint.payload.Bonus;
import com.kuuhaku.model.common.Hashable;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.enums.TrophyType;
import com.kuuhaku.model.persistent.id.CompositeMemberId;
import com.kuuhaku.utils.JSONObject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

@Entity
@Cacheable
@DynamicUpdate
@Table(name = "member")
@IdClass(CompositeMemberId.class)
public class Member implements Hashable {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String uid = "";

	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String sid = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String pseudoName = "";

	@Column(columnDefinition = "TEXT")
	private String pseudoAvatar = "";

	//NUMBERS
	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long xp = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long lastVoted = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long lastEarntXp = 0;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean rulesSent = false;

	@Enumerated(value = EnumType.STRING)
	private TrophyType trophy = null;

	@ElementCollection(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false, name = "member_id")
	private List<String> warns = new ArrayList<>();

	public Member(String uid, String sid) {
		this.uid = uid;
		this.sid = sid;
	}

	public Member() {

	}

	public static List<Bonus> getBonuses(User u) {
		List<Bonus> bonuses = new ArrayList<>();

		if (!getWaifu(u.getId()).isEmpty())
			bonuses.add(new Bonus(1, "Waifu", WaifuDAO.getMultiplier(u.getId()).getMult()));

		Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
		if (kp.getCards().size() / ((float) CardDAO.getTotalCards() * 2) >= 1)
			bonuses.add(new Bonus(4, "Coleção de cartas (100%)", 2));
		else if (kp.getCards().size() / ((float) CardDAO.getTotalCards() * 2) >= 0.75)
			bonuses.add(new Bonus(4, "Coleção de cartas (75%)", 1.75f));
		else if (kp.getCards().size() / ((float) CardDAO.getTotalCards() * 2) >= 0.5)
			bonuses.add(new Bonus(4, "Coleção de cartas (50%)", 1.5f));
		else if (kp.getCards().size() / ((float) CardDAO.getTotalCards() * 2) >= 0.25)
			bonuses.add(new Bonus(4, "Coleção de cartas (25%)", 1.25f));

		return bonuses;
	}

	public static String getWaifu(String id) {
		Couple c = WaifuDAO.getCouple(id);
		if (c == null) return "";
		return c.getHusbando().equals(id) ? c.getWaifu() : c.getHusbando();
	}

	public synchronized boolean addXp(Guild g, double buff) {
		AtomicReference<Double> mult = new AtomicReference<>(buff);

		boolean waifu = g.getMembers().stream().map(net.dv8tion.jda.api.entities.Member::getId).toList().contains(com.kuuhaku.model.persistent.Member.getWaifu(uid));
		if (waifu) {
			mult.updateAndGet(v -> v * WaifuDAO.getMultiplier(uid).getMult());
		}

		int level = getLevel();
		float spamModif = Math.max(0, Math.min((System.currentTimeMillis() - lastEarntXp) / 1000f, 1));
		xp += 15 * mult.get() * spamModif;
		lastEarntXp = System.currentTimeMillis();

		Account acc = AccountDAO.getAccount(uid);
		if (acc.hasPendingQuest()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.merge(DailyTask.XP_TASK, (int) Math.round(15 * mult.get() * spamModif), Integer::sum);
			acc.setDailyProgress(pg);
			AccountDAO.saveAccount(acc);
		}

		ClanMember cm = ClanDAO.getClanMember(uid);
		if (cm != null) {
			cm.addScore((long) (10 * mult.get() * spamModif));
			ClanDAO.saveMember(cm);
		}

		if (getLevel() > level) {
			acc.addCredit(75 + (8L * getLevel()), this.getClass());
			AccountDAO.saveAccount(acc);
			return true;
		}
		return false;
	}

	public synchronized long addXp(int amount) {
		int level = getLevel();
		float spamModif = Math.max(0, Math.min((System.currentTimeMillis() - lastEarntXp) / 10000f, 1));
		xp += amount * spamModif;
		lastEarntXp = System.currentTimeMillis();

		ClanMember cm = ClanDAO.getClanMember(uid);
		if (cm != null) {
			cm.addScore((long) (amount * spamModif));
			ClanDAO.saveMember(cm);
		}

		Account acc = AccountDAO.getAccount(uid);
		if (acc.hasPendingQuest()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.merge(DailyTask.XP_TASK, Math.round(amount * spamModif), Integer::sum);
			acc.setDailyProgress(pg);
			AccountDAO.saveAccount(acc);
		}

		if (getLevel() > level) {
			acc.addCredit(75 + (8L * getLevel()), this.getClass());
			AccountDAO.saveAccount(acc);
		}
		return (long) (amount * spamModif);
	}

	public void halfXp() {
		xp /= 2;
	}

	public void resetXp() {
		xp = 0;
	}

	public int getLevel() {
		return (int) Math.ceil(Math.sqrt(xp / 100f));
	}

	public long getXp() {
		return xp;
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

	public List<String> getWarns() {
		return warns;
	}

	public JSONObject toJson() {
		Account acc = AccountDAO.getAccount(uid);

		return new JSONObject() {{
			put("uid", uid);
			put("sid", sid);
			put("pseudoName", pseudoName);
			put("profileColor", acc.getProfileColor());
			put("bg", acc.getBg());
			put("bio", acc.getBio());
			put("pseudoAvatar", pseudoAvatar);
			put("level", getLevel());
			put("xp", xp);
			put("lastVoted", lastVoted);
			put("rulesSent", rulesSent);
		}};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Member member = (Member) o;
		return Objects.equals(uid, member.uid) && Objects.equals(sid, member.sid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid, sid);
	}

	@Override
	public String getHash() {
		CRC32 crc = new CRC32();
		crc.update(sid.getBytes(StandardCharsets.UTF_8));
		crc.update(uid.getBytes(StandardCharsets.UTF_8));

		return Long.toHexString(crc.getValue());
	}
}
