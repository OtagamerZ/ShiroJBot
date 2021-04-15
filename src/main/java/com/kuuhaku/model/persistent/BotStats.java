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
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import org.json.JSONObject;

import javax.persistence.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ExecutionException;

@Entity
@Table(name = "botstats")
public class BotStats {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Temporal(TemporalType.TIMESTAMP)
	private Date timestamp = Date.from(Instant.now(Clock.system(ZoneId.of("GMT-3"))));

	//INFO
	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long ping;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long memoryUsage;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private double memoryPrcnt;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private double cpuUsage;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int servers;

	public BotStats() {

	}

	public BotStats get() {
		memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		memoryPrcnt = Helper.prcnt(memoryUsage, Runtime.getRuntime().totalMemory());
		cpuUsage = ShiroInfo.getProcessCpuLoad();
		servers = Main.getShiroShards().getGuilds().size();

		long start = System.currentTimeMillis();
		try {
			Main.getShiroShards()
					.retrieveUserById(ShiroInfo.getNiiChan())
					.submit()
					.get();

			ping = System.currentTimeMillis() - start;
		} catch (InterruptedException | ExecutionException e) {
			ping = 0;
		}

		return this;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
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

	public int getServers() {
		return servers;
	}

	public void setServers(int servers) {
		this.servers = servers;
	}

	public long getPing() {
		return ping;
	}

	public void setPing(long ping) {
		this.ping = ping;
	}

	@Override
	public String toString() {
		return new JSONObject() {{
			put("id", id);
			put("timestamp", timestamp.getTime());
			put("ping", ping);
			put("memoryUsage", memoryUsage);
			put("memoryPrcnt", memoryPrcnt);
			put("cpuUsage", cpuUsage);
			put("servers", servers);
		}}.toString();
	}
}
