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

import com.kuuhaku.model.persistent.Expedition;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class ExpeditionDAO {
	public static Expedition getExpedition(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(Expedition.class, id);
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Expedition> getExpeditions() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e FROM Expedition e WHERE e.id <> 'DOJO'");

		try {
			return (List<Expedition>) q.getResultList();
		} finally {
			em.close();
		}
	}
}
