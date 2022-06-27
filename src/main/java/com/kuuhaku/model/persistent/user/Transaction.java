/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "transaction")
public class Transaction extends DAO<Transaction> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "value", nullable = false)
	private long value;

	@Column(name = "input", nullable = false)
	private boolean input;

	@Column(name = "reason", nullable = false)
	private String reason;

	@Column(name = "date", nullable = false)
	private ZonedDateTime date;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "account_uid")
	@Fetch(FetchMode.JOIN)
	private Account account;

	public Transaction() {
	}

	protected Transaction(Account account, long value, boolean input, String reason) {
		this.value = value;
		this.input = input;
		this.reason = reason;
		this.date = ZonedDateTime.now(ZoneId.of("GMT-3"));
		this.account = account;
	}

	public int getId() {
		return id;
	}

	public long getValue() {
		return value;
	}

	public boolean isInput() {
		return input;
	}

	public String getReason() {
		return reason;
	}

	public ZonedDateTime getDate() {
		return date;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}
}
