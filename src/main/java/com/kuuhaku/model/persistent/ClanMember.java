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

import com.kuuhaku.model.enums.ClanHierarchy;
import com.kuuhaku.utils.Helper;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "clanmember")
public class ClanMember {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Enumerated(value = EnumType.STRING)
	private ClanHierarchy role = ClanHierarchy.MEMBER;

	@Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime joinedAt = ZonedDateTime.now();

	public ClanMember(String uid, ClanHierarchy role) {
		this.uid = uid;
		this.role = role;
	}

	public ClanMember() {
	}

	public String getUid() {
		return uid;
	}

	public Clan getClan() {
		return clan;
	}

	public ClanHierarchy getRole() {
		return role;
	}

	public void setRole(ClanHierarchy role) {
		this.role = role;
	}

	public ZonedDateTime getJoinedAt() {
		return joinedAt;
	}

	public boolean isLeader() {
		return role == ClanHierarchy.LEADER;
	}

	public boolean isSubLeader() {
		return role == ClanHierarchy.SUBLEADER;
	}

	public void promote() {
		role = Helper.getNext(role, ClanHierarchy.MEMBER, ClanHierarchy.CAPTAIN, ClanHierarchy.SUBLEADER);
	}

	public void demote() {
		role = Helper.getPrevious(role, ClanHierarchy.MEMBER, ClanHierarchy.CAPTAIN, ClanHierarchy.SUBLEADER);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClanMember that = (ClanMember) o;
		return Objects.equals(uid, that.uid) && Objects.equals(clan, that.clan);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid, clan);
	}
}
