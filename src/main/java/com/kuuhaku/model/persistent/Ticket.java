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

import com.kuuhaku.utils.JSONObject;
import net.dv8tion.jda.api.entities.Member;

import javax.persistence.*;
import java.util.Map;

@Entity
@Table(name = "ticket")
public class Ticket {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int number;

	@Column(columnDefinition = "TEXT")
	private String subject = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String uid = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String sid = "";

	@Column(columnDefinition = "TEXT")
	private String msgId = "{}";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String invite = "";

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean solved = false;

	public Ticket(String subject, Member op) {
		this.subject = subject;
		this.uid = op.getId();
		this.sid = op.getGuild().getId();
	}

	public Ticket() {
	}

	public int getNumber() {
		return number;
	}

	public String getSubject() {
		return subject;
	}

	public String getUid() {
		return uid;
	}

	public String getSid() {
		return sid;
	}

	public void setMsgIds(Map<String, String> msgIds) {
		this.msgId = new JSONObject(msgIds).toString();
	}

	public Map<String, Object> getMsgIds() {
		return new JSONObject(msgId);
	}

	public String getInvite() {
		return invite;
	}

	public void setInvite(String invite) {
		this.invite = invite;
	}

	public boolean isSolved() {
		return solved;
	}

	public void solved() {
		this.solved = true;
	}
}
