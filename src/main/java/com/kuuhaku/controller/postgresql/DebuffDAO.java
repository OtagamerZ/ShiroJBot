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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Debuff;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class DebuffDAO {
	public static Debuff getDebuff(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(Debuff.class, id);
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Debuff> getDebuffs() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT d FROM Debuff d", Debuff.class);

		try {
			return (List<Debuff>) q.getResultList();
		} finally {
			em.close();
		}
	}
}