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

package com.kuuhaku.model.common.tournament;

import com.kuuhaku.utils.Helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Bracket {
	private final List<Phase> phases;

	public Bracket(int size) {
		this.phases = Arrays.asList(new Phase[(int) Helper.log(size, 2) + 1]);
		for (int i = 0; i < phases.size(); i++) {
			phases.set(i, new Phase((int) (size / Math.pow(2, i)), i == phases.size() - 1));
		}
	}

	public List<Phase> getPhases() {
		return phases;
	}

	public void populate(List<Participant> participants) {
		Phase phase = phases.get(0);
		for (int i = 0; i < phase.getParticipants().size(); i++) {
			phase.getParticipants().set(i, i >= participants.size() ? new Participant(null) : participants.get(i));
		}
		Collections.shuffle(phase.getParticipants());
	}
}
