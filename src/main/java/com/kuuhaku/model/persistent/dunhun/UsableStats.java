/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.dunhun.Usable;
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.context.SkillContext;
import com.kuuhaku.util.Utils;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;
import org.intellij.lang.annotations.Language;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Embeddable
@MappedSuperclass
public class UsableStats implements Serializable, Cloneable {
	@Language("Groovy")
	@Column(name = "targeter", columnDefinition = "TEXT")
	private String targeter;

	@Language("Groovy")
	@Column(name = "effect", nullable = false, columnDefinition = "TEXT")
	private String effect;

	public UsableStats() {
	}

	public List<Actor<?>> getTargets(Usable usable, Actor<?> source) {
		if (targeter == null) return List.of(source);

		SkillContext ctx = new SkillContext(source, null, usable);
		try {
			Utils.exec(usable.getId(), targeter, Map.of("ctx", ctx));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to load targets {}", usable.getId(), e);
		}

		return ctx.getValidTargets();
	}

	public String getEffect() {
		return effect;
	}

	public UsableStats copy() {
		try {
			return (UsableStats) clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
