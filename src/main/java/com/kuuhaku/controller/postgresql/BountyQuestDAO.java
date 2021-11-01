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

import com.kuuhaku.model.persistent.BountyQuest;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Set;

import static com.kuuhaku.model.enums.BountyDifficulty.*;

public class BountyQuestDAO {
	public static BountyQuest getBounty(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(BountyQuest.class, id);
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<BountyQuest> getBounties() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT b FROM BountyQuest b WHERE b.difficulty IN :allowed");
		q.setParameter("allowed", Set.of(VERY_EASY, EASY, MEDIUM, HARD, VERY_HARD));

		try {
			return (List<BountyQuest>) q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<BountyQuest> getTraining() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT b FROM BountyQuest b WHERE b.difficulty = 'NONE'");

		try {
			return (List<BountyQuest>) q.getResultList();
		} finally {
			em.close();
		}
	}
}
