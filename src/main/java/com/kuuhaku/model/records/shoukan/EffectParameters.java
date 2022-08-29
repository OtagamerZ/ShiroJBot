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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.model.enums.shoukan.TargetType;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.util.Utils;

import java.util.Arrays;

public record EffectParameters(Trigger trigger, Source source, Target... targets) {
	public EffectParameters(Trigger trigger) {
		this(trigger, new Source());
	}

	public EffectParameters(Trigger trigger, Target... targets) {
		this(trigger, new Source(),
				Arrays.stream(targets)
						.filter(t -> Utils.equalsAny(t.type(), TargetType.ALLY, TargetType.ENEMY))
						.toArray(Target[]::new)
		);
	}

	public int size() {
		int i = targets.length;
		if (source.card() != null) i++;

		return i;
	}

	public Target[] allies() {
		return Arrays.stream(targets)
				.filter(t -> t.type() == TargetType.ALLY)
				.toArray(Target[]::new);
	}

	public Target[] enemies() {
		return Arrays.stream(targets)
				.filter(t -> t.type() == TargetType.ENEMY)
				.toArray(Target[]::new);
	}
}
