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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class PStateDAO {
	public static double getInfluenceShare(ExceedEnum exceed) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT SUM(influence) FROM PoliticalState p", Long.class);

		try {
			float total = (long) q.getSingleResult();
			PoliticalState p = em.find(PoliticalState.class, exceed);
			if (p == null) {
				savePoliticalState(new PoliticalState(exceed));
				p = getPoliticalState(exceed);
			}
			return Helper.round(p.getInfluence() / total, 2);
		} finally {
			em.close();
		}
	}

	public static PoliticalState getPoliticalState(ExceedEnum exceed) {
		EntityManager em = Manager.getEntityManager();

		try {
			PoliticalState p = em.find(PoliticalState.class, exceed);
			if (p == null) {
				savePoliticalState(new PoliticalState(exceed));
				return getPoliticalState(exceed);
			} else return p;
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
