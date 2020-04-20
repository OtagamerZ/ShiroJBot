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

import com.kuuhaku.model.common.Exceed;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.model.persistent.MonthWinner;
import com.kuuhaku.utils.ExceedEnums;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.time.LocalDate;
import java.util.List;

public class ExceedDAO {
	@SuppressWarnings("unchecked")
	public static List<Member> getExceedMembers(ExceedEnums ex) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m WHERE exceed LIKE ?1", Member.class);
		q.setParameter(1, ex.getName());

		List<Member> members = (List<Member>) q.getResultList();
		em.close();

		return members;
	}

	@SuppressWarnings("unchecked")
	public static Exceed getExceed(ExceedEnums ex) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m WHERE exceed LIKE ?1", Member.class);
		q.setParameter(1, ex.getName());

		List<Member> members = (List<Member>) q.getResultList();
		em.close();

		return new Exceed(ex, members.size(), members.stream().mapToLong(Member::getXp).sum());
	}

	public static ExceedEnums findWinner() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT exceed FROM Member m WHERE exceed NOT LIKE '' GROUP BY exceed ORDER BY xp DESC", String.class);
		q.setMaxResults(1);

		String winner = (String) q.getSingleResult();
		em.close();

		return ExceedEnums.getByName(winner);
	}

	public static void markWinner(ExceedEnums ex) {
		EntityManager em = Manager.getEntityManager();

		MonthWinner m = new MonthWinner();
		m.setExceed(ex.getName());

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static String getWinner() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT w FROM MonthWinner w ORDER BY id DESC", MonthWinner.class);
		q.setMaxResults(1);
		try {
			MonthWinner winner = (MonthWinner) q.getSingleResult();
			em.close();

			if (LocalDate.now().isBefore(winner.getExpiry())) {
				return winner.getExceed();
			} else {
				return "none";
			}
		} catch (NoResultException | IndexOutOfBoundsException e) {
			em.close();
			return "none";
		}
	}
}
