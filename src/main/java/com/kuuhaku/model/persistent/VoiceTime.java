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

import com.kuuhaku.model.common.Hashable;
import com.kuuhaku.model.persistent.id.CompositeMemberId;
import com.kuuhaku.utils.Helper;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.CRC32;

@Entity
@Table(name = "voicetime")
@IdClass(CompositeMemberId.class)
public class VoiceTime implements Hashable {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String sid;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long time = 0;

	private transient int joinOffset = (int) (System.currentTimeMillis() % 60000);

	public VoiceTime(String uid, String sid) {
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

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VoiceTime voiceTime = (VoiceTime) o;
		return Objects.equals(uid, voiceTime.uid) && Objects.equals(sid, voiceTime.sid);
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
