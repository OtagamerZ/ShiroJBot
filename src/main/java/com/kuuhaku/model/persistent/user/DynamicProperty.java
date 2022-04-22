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
import com.kuuhaku.model.persistent.id.DynamicPropertyId;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;

@Entity
@Table(name = "dynamic_property")
public class DynamicProperty extends DAO {
	@EmbeddedId
	private DynamicPropertyId id;

	@Column(name = "value", nullable = false)
	private String value;

	@ManyToOne(optional = false)
	@JoinColumn(name = "uid", nullable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("uid")
	private Account account;

	public DynamicProperty() {
	}

	public DynamicProperty(Account account, String id, String value) {
		this.id = new DynamicPropertyId(account.getUid(), id);
		this.value = value;
		this.account = account;
	}

	public DynamicPropertyId getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Account getAccount() {
		return account;
	}
}
