/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model;

import com.kuuhaku.Main;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table
public class Member {
	@Id
	private String id;
	private String mid;
	private int level = 1, xp = 0;
	private String warns = "";
	private String bg = "https://pm1.narvii.com/6429/7f50ee6d5a42723882c6c23a8420f24dfff60e4f_hq.jpg";
	private String bio = "";
	@Column(columnDefinition = "String default \"\"")
	private String waifu = "";
	@Column(columnDefinition = "String default \"\"")
	private String exceed = "";
	@Column(columnDefinition = "boolean default false")
	private boolean markForDelete;
	@Column(columnDefinition = "boolean default false")
	private boolean rulesSent;

	public Member() {

	}

	public boolean addXp() {
		xp += Main.getInfo().getWinner().equals(this.exceed) ? 30 : 15;
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

	public void addWarn(String reason) {
		List<String> ph = new ArrayList<>(Arrays.asList(getWarns()));
		ph.add(reason);
		warns = ph.toString();
	}

	public void removeWarn(int index) {
		List<String> ph = new ArrayList<>(Arrays.asList(getWarns()));
		ph.remove(index);
		warns = ph.toString();
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

	private String[] getWarns() {
		return warns.replace("[", "").replace("]", "").split(",");
	}

	public void setId(String id) {
		this.id = id;
	}

	String getBg() {
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

	String getBio() {
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

	public void setWaifu(User waifu) {
		this.waifu = waifu.getId();
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
}
