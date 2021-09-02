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

import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "phase")
public class Phase {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int phase;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "phase_id")
	private List<Participant> participants;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean last;

	public Phase() {
	}

	public Phase(int phase, int size, boolean last) {
		this.phase = phase;
		this.participants = Arrays.asList(new Participant[size]);
		this.last = last;
	}

	public int getId() {
		return id;
	}

	public int getPhase() {
		return phase;
	}

	public int getSize() {
		return participants.size();
	}

	public List<Participant> getParticipants() {
		return participants;
	}

	public Pair<Participant, Participant> getMatch(int index) {
		boolean top = index % 2 == 0;

		return Pair.of(
				participants.get(top ? index : index - 1),
				participants.get(top ? index + 1 : index)
		);
	}

	public void setMatch(int index, Participant p) {
		p.setIndex(index);
		participants.set(index, p);
	}

	public boolean isLast() {
		return last;
	}

	public Participant getOpponent(Participant p) {
		boolean top = p.getIndex() % 2 == 0;

		if (last) return null;
		return participants.get(top ? p.getIndex() + 1 : p.getIndex() - 1);
	}
}
