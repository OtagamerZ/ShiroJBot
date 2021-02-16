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

import com.kuuhaku.model.enums.DonationBundle;
import com.kuuhaku.utils.Helper;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "donation")
public class Donation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String transaction = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String uid = "";

	@Enumerated(value = EnumType.STRING)
	private DonationBundle bundle;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private float value = 0;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String date = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3")).format(Helper.dateformat);

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String status = "";

	public Donation(String transaction, String uid, DonationBundle bundle, float value, String status) {
		this.transaction = transaction;
		this.uid = uid;
		this.bundle = bundle;
		this.value = value;
		this.status = status;
	}

	public Donation() {
	}

	public int getId() {
		return id;
	}

	public String getTransaction() {
		return transaction;
	}

	public void setTransaction(String transaction) {
		this.transaction = transaction;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public DonationBundle getBundle() {
		return bundle;
	}

	public void setBundle(DonationBundle bundle) {
		this.bundle = bundle;
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
