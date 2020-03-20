/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.controller.mysql;

import com.kuuhaku.model.persistent.AnsweredQuizzes;
import com.kuuhaku.model.persistent.Quiz;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class QuizDAO {
	public static void saveQuiz(Quiz q) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(q);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static Quiz getRandomQuiz() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT q FROM Quiz q", Quiz.class);
		List<Quiz> quizzes = (List<Quiz>) q.getResultList();

		em.close();

		return quizzes.get(Helper.rng(quizzes.size()));
	}

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
