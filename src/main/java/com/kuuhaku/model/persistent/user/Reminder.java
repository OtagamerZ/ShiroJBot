/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.converter.ChannelConverter;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "reminder")
public class Reminder extends DAO<Reminder> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Lob
	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(name = "due", nullable = false)
	private ZonedDateTime due;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "account_uid")
	@Fetch(FetchMode.JOIN)
	private Account account;

	@Column(name = "channel")
	@Convert(converter = ChannelConverter.class)
	private GuildMessageChannel channel;

	@Column(name = "reminded", nullable = false)
	private boolean reminded;

	public Reminder() {
	}

	public Reminder(Account account, GuildMessageChannel channel, String message, long offset) {
		this.message = message;
		this.due = ZonedDateTime.now(ZoneId.of("GMT-3")).plus(offset, ChronoUnit.MILLIS);
		this.account = account;
		this.channel = channel;
	}

	public int getId() {
		return id;
	}

	public String getMessage() {
		return message;
	}

	public ZonedDateTime getDue() {
		return due;
	}

	public Account getAccount() {
		return account;
	}

	public GuildMessageChannel getChannel() {
		return channel;
	}

	public boolean wasReminded() {
		return reminded;
	}

	public void setReminded(boolean reminded) {
		this.reminded = reminded;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Reminder reminder = (Reminder) o;
		return id == reminder.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
