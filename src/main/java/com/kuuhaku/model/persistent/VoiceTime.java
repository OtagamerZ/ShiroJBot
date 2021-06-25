/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.utils.Helper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "voicetime")
public class VoiceTime {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String sid;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long time = 0;

	private transient int joinOffset = (int) (System.currentTimeMillis() % 60000);

	public VoiceTime(String uid, String sid) {
		this.id = uid + sid;
		this.uid = uid;
		this.sid = sid;
	}

	public VoiceTime() {
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public long getTime() {
		return time;
	}

	public void update() {
		if (joinOffset > 0) {
			this.time += 60000 - joinOffset;
			joinOffset = 0;
		} else
			this.time += 60000;
	}

	public String getReadableTime() {
		return Helper.toStringDuration(time);
	}
}
