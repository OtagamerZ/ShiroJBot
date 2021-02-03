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

import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@DynamicUpdate
@Table(name = "exceedmember")
public class ExceedMember {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String exceed = "";

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long contribution = 0;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean blocked = false;

	public ExceedMember(String id, String ex) {
		this.id = id;
		this.exceed = ex;
	}

	public ExceedMember() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getExceed() {
		return exceed;
	}

	public void setExceed(String exceed) {
		this.exceed = exceed;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

	public long getContribution() {
		return contribution;
	}

	public void addContribution(long amount) {
		this.contribution += amount;
	}

	public void resetContribution() {
		this.contribution = 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExceedMember that = (ExceedMember) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
