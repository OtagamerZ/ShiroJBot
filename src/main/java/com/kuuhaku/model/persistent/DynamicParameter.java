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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.annotations.WhenNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dynamicparameter")
public class DynamicParameter extends DAO {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String param = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String value = "";

	public DynamicParameter() {
	}

	@WhenNull
	public DynamicParameter(String param) {
		this.param = param;
	}

	public DynamicParameter(String param, Object value) {
		this.param = param;
		this.value = String.valueOf(value);
	}

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

	public String getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = String.valueOf(value);
	}
}
