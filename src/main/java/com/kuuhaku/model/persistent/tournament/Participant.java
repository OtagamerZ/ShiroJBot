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

import com.kuuhaku.utils.Helper;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "participant")
public class Participant {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String uid;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int index = -1;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int points = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int phase = 0;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean wo = false;

	public transient static final Participant BYE = new Participant(null);

	public Participant() {
	}

	public Participant(String uid) {
		if (uid == null) {
			uid = "BYE";
			id = -1;
		}

		this.uid = uid;
	}

	public int getId() {
		return id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
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

	public int getPhase() {
		return phase;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public boolean isBye() {
		return id == -1 || uid.equals("BYE");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Participant that = (Participant) o;
		return id == that.id && index == that.index && points == that.points && phase == that.phase && Objects.equals(uid, that.uid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, uid, index, points, phase);
	}

	@Override
	public String toString() {
		return isBye() ? "BYE" : Helper.getUsername(uid);
	}
}
