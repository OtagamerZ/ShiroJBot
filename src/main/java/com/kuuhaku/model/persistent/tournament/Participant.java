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

package com.kuuhaku.model.persistent.tournament;

import com.kuuhaku.model.persistent.id.CompositeTournamentId;
import com.kuuhaku.utils.Helper;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "participant")
@IdClass(CompositeTournamentId.class)
public class Participant {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String uid;

	@Id
	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int tournament;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int index = -1;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int points = 0;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean third = false;

	public Participant() {
	}

	public Participant(String uid, Tournament t) {
		if (uid == null) uid = "BYE";
		this.uid = uid;
		this.tournament = t.getId();
	}

	public String getUid() {
		return uid;
	}

	public int getTournament() {
		return tournament;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public void addPoints(int pts) {
		points += pts;
	}

	public boolean isThird() {
		return third;
	}

	public void setThird() {
		this.third = true;
	}

	public boolean isBye() {
		return uid.equals("BYE");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Participant that = (Participant) o;
		return tournament == that.tournament && Objects.equals(uid, that.uid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid, tournament);
	}

	@Override
	public String toString() {
		return isBye() ? "BYE" : Helper.getUsername(uid);
	}
}
