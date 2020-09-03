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

import com.kuuhaku.handlers.api.endpoint.ExceedState;
import com.kuuhaku.model.common.Exceed;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.model.persistent.ExceedScore;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.model.persistent.MonthWinner;
import com.kuuhaku.utils.ExceedEnum;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ExceedDAO {
	public static void unblock() {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.createQuery("UPDATE ExceedMember em SET em.blocked = false WHERE em.blocked = true").executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	public static boolean hasExceed(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			ExceedMember ex = em.find(ExceedMember.class, id);
			return ex != null && !ex.getExceed().isBlank();
		} finally {
			em.close();
		}
	}

	public static String getExceed(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(ExceedMember.class, id).getExceed();
		} catch (NullPointerException e) {
			return "";
		} finally {
			em.close();
		}
	}

	public static ExceedState getExceedState(String exceed) {
		if (exceed.isBlank()) return new ExceedState(-1, "", 0);

		@SuppressWarnings("SuspiciousMethodCalls")
		int pos = Arrays.stream(ExceedEnum.values())
						  .map(ExceedDAO::getExceed)
						  .sorted(Comparator.comparingLong(Exceed::getExp).reversed())
						  .collect(Collectors.toList())
						  .indexOf(ExceedEnum.getByName(exceed)) + 1;

		return new ExceedState(ExceedEnum.getByName(exceed).ordinal(), exceed, pos);
	}

	public static void joinExceed(ExceedMember ex) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(ex);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<ExceedMember> getExceedMembers(ExceedEnum ex) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT ex FROM ExceedMember ex WHERE ex.exceed = :exceed", ExceedMember.class);
		q.setParameter("exceed", ex.getName());

		List<ExceedMember> members = (List<ExceedMember>) q.getResultList();
		em.close();

		return members;
	}

	@SuppressWarnings("unchecked")
	public static List<ExceedMember> getExceedMembers() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT ex FROM ExceedMember ex", ExceedMember.class);

		List<ExceedMember> members = (List<ExceedMember>) q.getResultList();
		em.close();

		return members;
	}

	public static ExceedMember getExceedMember(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(ExceedMember.class, id);
		} finally {
			em.close();
		}
	}

	@SuppressWarnings({"unchecked", "SqlResolve"})
	public static Exceed getExceed(ExceedEnum ex) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m INNER JOIN ExceedMember ex ON m.mid = ex.id WHERE ex.exceed = :exceed", Member.class);
		q.setParameter("exceed", ex.getName());

		Query points = em.createNativeQuery("SELECT points FROM shiro.\"GetExceedRanking\" WHERE exceed = :exceed");
		points.setParameter("exceed", ex.getName());

		List<Member> members = (List<Member>) q.getResultList();

		long offset = 0;
		MonthWinner ranking = getLatestRanking();
		if (ranking != null)
			switch (ex) {
				case IMANITY:
					offset = ranking.getImanityPoints();
					break;
				case SEIREN:
					offset = ranking.getSeirenPoints();
					break;
				case WEREBEAST:
					offset = ranking.getWerebeastPoints();
					break;
				case ELF:
					offset = ranking.getElfPoints();
					break;
				case EXMACHINA:
					offset = ranking.getExmachinaPoints();
					break;
				case FLUGEL:
					offset = ranking.getFlugelPoints();
					break;
			}

		try {
			return new Exceed(ex, members.size(), ((BigDecimal) points.getSingleResult()).longValue() - offset);
		} finally {
			em.close();
		}
	}

	@SuppressWarnings({"SqlResolve", "unchecked"})
	public static ExceedEnum findWinner() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT er.exceed, er.points FROM shiro.\"GetExceedRanking\" er ORDER BY er.points DESC");

		List<Object[]> ex = (List<Object[]>) q.getResultList();
		em.close();

		Map<String, Long> exs = new HashMap<>();
		for (Object[] objects : ex)
			exs.put(String.valueOf(objects[0]), ((BigDecimal) objects[1]).longValue() - getExceedHistory(ExceedEnum.getByName(String.valueOf(objects[0]))).get(0).getPoints());

		Map.Entry<String, Long> winner = exs.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();

		return Arrays.stream(ExceedEnum.values()).filter(e -> e.name().startsWith(winner.getKey())).findFirst().orElseThrow();
	}

	public static void markWinner(ExceedEnum ex) {
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

			if (LocalDate.now().isBefore(winner.getExpiry())) {
				return winner.getExceed();
			} else {
				return "none";
			}
		} catch (NoResultException | IndexOutOfBoundsException e) {
			return "none";
		} finally {
			em.close();
		}
	}

	public static MonthWinner getLatestRanking() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT w FROM MonthWinner w ORDER BY id DESC", MonthWinner.class);
		q.setMaxResults(1);

		try {
			MonthWinner winner = (MonthWinner) q.getSingleResult();

			return winner;
		} catch (NoResultException | IndexOutOfBoundsException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static String getLeader(ExceedEnum ex) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m.mid FROM Member m WHERE m.mid IN (SELECT em.id FROM ExceedMember em WHERE em.exceed = :exceed) GROUP BY m.mid ORDER BY SUM(m.xp) DESC", String.class);
		q.setParameter("exceed", ex.getName());
		q.setMaxResults(1);

		try {
			return String.valueOf(q.getSingleResult());
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static float getPercentage(ExceedEnum ex) {
		EntityManager em = Manager.getEntityManager();

		Query exceed = em.createQuery("SELECT COUNT(e) FROM ExceedMember e WHERE e.exceed = :ex", Long.class);
		Query total = em.createQuery("SELECT COUNT(e) FROM ExceedMember e", Long.class);
		exceed.setParameter("ex", ex.getName());

		try {
			return ((Long) exceed.getSingleResult()).floatValue() / ((Long) total.getSingleResult()).floatValue();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings({"unchecked"})
	public static List<ExceedScore> getExceedHistory(ExceedEnum ex) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e FROM ExceedScore e WHERE e.exceed = :ex", ExceedScore.class);
		q.setParameter("ex", ex.getName());

		try {
			return (List<ExceedScore>) q.getResultList();
		} finally {
			em.close();
		}
	}
}
