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
import org.apache.commons.collections4.ListUtils;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "bracket")
public class Bracket {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int size;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "bracket_id")
	private List<Phase> phases;

	public Bracket() {
	}

	public Bracket(int size) {
		this.size = size;
		this.phases = new ArrayList<>(Arrays.asList(new Phase[(int) Helper.log(size, 2) + 1]));
		for (int i = 0; i < phases.size(); i++) {
			phases.set(i, new Phase(i, (int) (size / Math.pow(2, i)), i == phases.size() - 1));
		}
		this.phases.sort(Comparator.comparingInt(Phase::getPhase));
	}

	public int getSize() {
		return size;
	}

	public List<Phase> getPhases() {
		phases.sort(Comparator.comparingInt(Phase::getPhase));
		return phases;
	}

	public void populate(Tournament t, List<Participant> participants) {
		List<String> ids = participants.stream()
				.map(Participant::getUid)
				.collect(Collectors.toList());

		if (participants.size() >= size)
			ids = ids.subList(0, size);
		else
			ids.addAll(ListUtils.union(ids, Collections.nCopies(size - participants.size(), "BYE")));

		Collections.shuffle(ids);
		getPhases().get(0).setParticipants(ids);

		for (int j = 0; j < phases.size(); j++) {
			Phase phase = getPhases().get(j);
			for (int i = 0; i < phase.getSize(); i++) {
				Participant p = t.getLookup(phase.getParticipants().get(i));
				if (p == null) continue;

				if (p.getIndex() == -1) p.setIndex(i);
				Participant op = t.getLookup(phase.getOpponent(p));
				if (op != null && op.isBye()) {
					t.setResult(j, i);
				}
			}
		}
	}
}
