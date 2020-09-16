/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.controller.sqlite;

import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.enums.ExceedEnum;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class PStateDAO {
	@SuppressWarnings("unchecked")
	public static List<PoliticalState> getAllPoliticalState() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT p FROM PoliticalState p", PoliticalState.class);

		try {
			return (List<PoliticalState>) q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	public static PoliticalState getPoliticalState(ExceedEnum exceed) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(PoliticalState.class, exceed);
		} finally {
			em.close();
		}
	}

	public static void savePoliticalState(PoliticalState p) {
		EntityManager em = Manager.getEntityManager();

		if (p.getInfluence() < 0) p.modifyInfluence(-p.getInfluence());

		em.getTransaction().begin();
		em.merge(p);
		em.getTransaction().commit();

		em.close();
	}
}
