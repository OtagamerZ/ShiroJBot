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
import com.kuuhaku.controller.postgresql.Manager;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.ShiroInfo;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "botstats")
public class BotStats {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime timestamp = ZonedDateTime.now(ZoneId.of("GMT-3"));

	//INFO
	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long ping;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long dbPing;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long memoryUsage;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private double memoryPrcnt;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private double cpuUsage;

	//CACHES
	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int ratelimitCount;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int confirmationPendingCount;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int specialEventCount;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int currentCardCount;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int currentDropCount;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int cardCacheCount;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int resourceCacheCount;

	private transient long averageMemory = 0;

	public BotStats() {

	}

	public BotStats get() {
		memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		memoryPrcnt = Helper.prcnt(memoryUsage, ShiroInfo.getSystemInfo().getTotalMemorySize());
		cpuUsage = ShiroInfo.getSystemInfo().getProcessCpuLoad();
		try {
			ping = Main.getShiroShards().getShards().get(0).getRestPing().complete();
		} catch (Exception e) {
			ping = 0;
		}
		dbPing = Manager.ping();

		ratelimitCount = Main.getInfo().getRatelimit().size();
		confirmationPendingCount = Main.getInfo().getConfirmationPending().size();
		specialEventCount = Main.getInfo().getSpecialEvent().size();
		currentCardCount = Main.getInfo().getCurrentCard().size();
		currentDropCount = Main.getInfo().getCurrentDrop().size();

		return this;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(ZonedDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public long getPing() {
		return ping;
	}

	public void setPing(long ping) {
		this.ping = ping;
	}

	public long getDbPing() {
		return dbPing;
	}

	public void setDbPing(long dbPing) {
		this.dbPing = dbPing;
	}

	public long getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(long memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public double getMemoryPrcnt() {
		return memoryPrcnt;
	}

	public void setMemoryPrcnt(double memoryPrcnt) {
		this.memoryPrcnt = memoryPrcnt;
	}

	public double getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(double cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	public int getRatelimitCount() {
		return ratelimitCount;
	}

	public void setRatelimitCount(int ratelimitCount) {
		this.ratelimitCount = ratelimitCount;
	}

	public int getConfirmationPendingCount() {
		return confirmationPendingCount;
	}

	public void setConfirmationPendingCount(int confirmationPendingCount) {
		this.confirmationPendingCount = confirmationPendingCount;
	}

	public int getSpecialEventCount() {
		return specialEventCount;
	}

	public void setSpecialEventCount(int specialEventCount) {
		this.specialEventCount = specialEventCount;
	}

	public int getCurrentCardCount() {
		return currentCardCount;
	}

	public void setCurrentCardCount(int currentCardCount) {
		this.currentCardCount = currentCardCount;
	}

	public int getCurrentDropCount() {
		return currentDropCount;
	}

	public void setCurrentDropCount(int currentDropCount) {
		this.currentDropCount = currentDropCount;
	}

	public int getCardCacheCount() {
		return cardCacheCount;
	}

	public void setCardCacheCount(int cardCacheCount) {
		this.cardCacheCount = cardCacheCount;
	}

	public int getResourceCacheCount() {
		return resourceCacheCount;
	}

	public void setResourceCacheCount(int resourceCacheCount) {
		this.resourceCacheCount = resourceCacheCount;
	}

	public long getAverageMemory() {
		return averageMemory == 0 ? memoryUsage : averageMemory;
	}

	public void setAverageMemory(long averageMemory) {
		this.averageMemory = averageMemory;
	}

	@Override
	public String toString() {
		return new JSONObject() {{
			put("id", id);
			put("timestamp", timestamp.toInstant().toEpochMilli());
			put("ping", ping);
			put("dbPing", dbPing);
			put("memoryUsage", memoryUsage);
			put("memoryPrcnt", memoryPrcnt);
			put("cpuUsage", cpuUsage);
		}}.toString();
	}
}
