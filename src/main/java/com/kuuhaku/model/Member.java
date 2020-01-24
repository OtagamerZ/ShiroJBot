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

package com.kuuhaku.model;

import com.kuuhaku.Main;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.stream.Collectors;

@Entity
public class Member {
	@Id
	private String id;
	private String mid;
	private String sid;
	private int level = 1, xp = 0;
	@Column(columnDefinition = "float default 1.25")
	private float waifuMult = 1.25f;
	private String bg = "https://pm1.narvii.com/6429/7f50ee6d5a42723882c6c23a8420f24dfff60e4f_hq.jpg";
	private String bio = "";
	private String waifu = "";
	private String exceed = "";
	@Column(columnDefinition = "boolean default false")
	private boolean markForDelete;
	@Column(columnDefinition = "boolean default false")
	private boolean rulesSent;
	@Column(columnDefinition = "long default 0")
	private long lastVoted;

	public Member() {

	}

	public boolean addXp(Guild g) {
		if (g.getMembers().stream().map(net.dv8tion.jda.api.entities.Member::getId).collect(Collectors.toList()).contains(waifu)) {
			xp += (Main.getInfo().getWinner().equals(this.exceed) ? 30 : 15) * waifuMult;
		} else xp += (Main.getInfo().getWinner().equals(this.exceed) ? 30 : 15);

		if (xp >= (int) Math.pow(level, 2) * 100) {
			level++;
			return true;
		}
		return false;
	}

	public void resetXp() {
		level = 1;
		xp = 0;
	}

	public String getId() {
		return id;
	}

	public int getLevel() {
		return level;
	}

	public int getXp() {
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

	public String getWaifu() {
		if (waifu == null) waifu = "";
		return waifu;
	}

	public void marry(User waifu) {
		this.waifu = waifu.getId();
	}

	public void divorce() {
		this.waifu = "";
		this.waifuMult *= 0.99f;
		Helper.clamp(this.waifuMult, 1.05f, 1.25f);
	}

	public String getExceed() {
		return exceed;
	}

	public void setExceed(String exceed) {
		this.exceed = exceed;
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

	public float getWaifuMult() {
		return waifuMult;
	}

	public long getLastVoted() {
		return lastVoted;
	}
}
