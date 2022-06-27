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

package com.kuuhaku.model.enums;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.user.StashedCard;
import org.intellij.lang.annotations.Language;

public enum CardType {
	NONE,
	KAWAIPON(StashedCard.class, "SELECT sc FROM StashedCard sc WHERE sc.kawaiponCard.uuid = ?1"),
	EVOGEAR,
	FIELD;

	private final Class<? extends DAO<?>> klass;
	private final String query;

	CardType() {
		this.klass = null;
		this.query = null;
	}

	CardType(Class<? extends DAO<?>> klass, @Language("JPAQL") String query) {
		this.klass = klass;
		this.query = query;
	}

	public Class<? extends DAO<?>> getKlass() {
		return klass;
	}

	public String getQuery() {
		return query;
	}
}
