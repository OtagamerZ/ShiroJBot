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
import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.api.endpoint.Bonus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import javax.persistence.Version;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Entity
@Table(name = "member")
@OptimisticLocking(type = OptimisticLockType.VERSION)
public class Member {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String mid = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String sid = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String pseudoName = "";

	@Column(columnDefinition = "VARCHAR(7) NOT NULL DEFAULT ''")
	private String profileColor = "";


	//TEXTS
	@Column(columnDefinition = "TEXT")
	private String bg = "https://pm1.narvii.com/6429/7f50ee6d5a42723882c6c23a8420f24dfff60e4f_hq.jpg";

	@Column(columnDefinition = "TEXT")
	private String bio = "";

	@Column(columnDefinition = "TEXT")
	private String pseudoAvatar = "";

	//NUMBERS
	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int level = 1;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long xp = 0;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long lastVoted = 0;

	@Version
	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long version = 0;

	//SWITCHES
	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean markForDelete = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean rulesSent = false;

	public Member() {

	}

	public static List<Bonus> getBonuses(User u) {
		List<Bonus> bonuses = new ArrayList<>();

		if (ExceedDAO.hasExceed(u.getId()) && Main.getInfo().getWinner().equals(ExceedDAO.getExceed(u.getId())))
			bonuses.add(new Bonus(0, "Exceed Vitorioso", 2));
		if (!getWaifu(u).isEmpty())
			bonuses.add(new Bonus(1, "Waifu", WaifuDAO.getMultiplier(u).getMult()));
		if (KGotchiDAO.getKawaigotchi(u.getId()) != null && !Objects.requireNonNull(KGotchiDAO.getKawaigotchi(u.getId())).isAlive())
			bonuses.add(new Bonus(2, "Kawaigotchi", Objects.requireNonNull(KGotchiDAO.getKawaigotchi(u.getId())).getTier().getUserXpMult()));
		else if (KGotchiDAO.getKawaigotchi(u.getId()) != null)
			bonuses.add(new Bonus(3, "Kawaigotchi Morto", 0.8f));

		Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
		if (kp.getCards().size() / (float) CardDAO.totalCards() >= 1)
			bonuses.add(new Bonus(4, "Coleção de cartas (100%)", 2));
		else if (kp.getCards().size() / (float) CardDAO.totalCards() >= 0.75)
			bonuses.add(new Bonus(4, "Coleção de cartas (75%)", 1.75f));
		else if (kp.getCards().size() / (float) CardDAO.totalCards() >= 0.5)
			bonuses.add(new Bonus(4, "Coleção de cartas (50%)", 1.5f));
		else if (kp.getCards().size() / (float) CardDAO.totalCards() >= 0.25)
			bonuses.add(new Bonus(4, "Coleção de cartas (25%)", 1.25f));

		return bonuses;
	}

	public static String getWaifu(User u) {
		Couple c = WaifuDAO.getCouple(u);
		if (c == null) return "";
		return c.getHusbando().equals(u.getId()) ? c.getWaifu() : c.getHusbando();
	}

	public boolean addXp(Guild g) {
		User u = Main.getInfo().getUserByID(mid);
		AtomicReference<Double> mult = new AtomicReference<>(1d);

		if (ExceedDAO.hasExceed(mid) && Main.getInfo().getWinner().equals(ExceedDAO.getExceed(mid)))
			mult.updateAndGet(v -> v * 2);
		if (g.getMembers().stream().map(net.dv8tion.jda.api.entities.Member::getId).collect(Collectors.toList()).contains(Member.getWaifu(u)))
			mult.updateAndGet(v -> v * WaifuDAO.getMultiplier(u).getMult());
		if (KGotchiDAO.getKawaigotchi(mid) != null && !Objects.requireNonNull(KGotchiDAO.getKawaigotchi(mid)).isAlive())
			mult.updateAndGet(v -> v * 0.8f);
		else if (KGotchiDAO.getKawaigotchi(mid) != null)
			mult.updateAndGet(v -> v * Objects.requireNonNull(KGotchiDAO.getKawaigotchi(mid)).getTier().getUserXpMult());

		Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
		if (kp.getCards().size() / (float) CardDAO.totalCards() >= 1) {
			mult.updateAndGet(v -> v * 1.5f);
		} else if (kp.getCards().size() / (float) CardDAO.totalCards() >= 0.75)
			mult.updateAndGet(v -> v * 1.37f);
		else if (kp.getCards().size() / (float) CardDAO.totalCards() >= 0.5)
			mult.updateAndGet(v -> v * 1.25f);
		else if (kp.getCards().size() / (float) CardDAO.totalCards() >= 0.25)
			mult.updateAndGet(v -> v * 1.12f);

		GuildBuff gb = GuildBuffDAO.getBuffs(g.getId());
		gb.getBuffs().stream().filter(b -> b.getId() == 1).findAny().ifPresent(b -> mult.updateAndGet(v -> v * b.getMult()));

		xp += 15 * mult.get();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (xp >= (int) Math.pow(level, 2) * 100) {
			level++;
			Account acc = AccountDAO.getAccount(mid);
			acc.addCredit(75 + (10 * level), this.getClass());
			AccountDAO.saveAccount(acc);
			return true;
		}
		return false;
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

	public String getProfileColor() {
		return profileColor;
	}

	public void setProfileColor(String profileColor) {
		this.profileColor = profileColor;
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

	public String getBg() {
		return bg;
	}

	public void setBg(String bg) {
		this.bg = bg;
	}

	public boolean isMarkForDelete() {
		return markForDelete;
	}

	public void setMarkForDelete(boolean markForDelete) {
		this.markForDelete = markForDelete;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getMid() {
		return mid;
	}

	public void setMid(String mid) {
		this.mid = mid;
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

	public int getLocalRank() {
		List<Member> mbs = MemberDAO.getMemberRank(sid, false);
		int pos = 0;
		for (int i = 0; i < mbs.size(); i++) {
			if (mbs.get(i).getId().equals(id)) {
				pos = i + 1;
				break;
			}
		}

		return pos;
	}

	public int getGlobalRank() {
		List<Member> mbs = MemberDAO.getMemberRank(sid, true);
		int posG = 0;
		for (int i = 0; i < mbs.size(); i++) {
			if (mbs.get(i).getId().equals(id)) {
				posG = i + 1;
				break;
			}
		}

		return posG;
	}
}
