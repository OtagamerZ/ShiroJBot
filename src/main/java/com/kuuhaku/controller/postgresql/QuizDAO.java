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

import com.kuuhaku.model.persistent.AnsweredQuizzes;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;

public class QuizDAO {
	public static AnsweredQuizzes getUserState(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return Helper.getOr(em.find(AnsweredQuizzes.class, id), new AnsweredQuizzes(id));
		} finally {
			em.close();
		}
	}

	public static void saveUserState(AnsweredQuizzes aq) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(aq);
		em.getTransaction().commit();

		em.close();
	}

	public static void resetUserStates() {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.createQuery("DELETE FROM AnsweredQuizzes").executeUpdate();
		em.getTransaction().commit();

		em.close();
	}
}
