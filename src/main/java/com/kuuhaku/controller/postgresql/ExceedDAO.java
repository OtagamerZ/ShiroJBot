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
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.model.persistent.ExceedScore;
import com.kuuhaku.model.persistent.MonthWinner;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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

    public static void removeMember(ExceedMember ex) {
        EntityManager em = Manager.getEntityManager();

        em.getTransaction().begin();
        Query q = em.createQuery("DELETE FROM Member WHERE id = :id");
        q.setParameter("id", ex.getId());
        q.executeUpdate();
        em.getTransaction().commit();

        em.close();
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

    public static void saveExceedMember(ExceedMember ex) {
        if (BlacklistDAO.isBlacklisted(ex.getId())) return;
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

        List<ExceedMember> members = q.getResultList();
        em.close();

        return members;
    }

    @SuppressWarnings("unchecked")
    public static List<ExceedMember> getExceedMembers() {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createQuery("SELECT ex FROM ExceedMember ex", ExceedMember.class);

        List<ExceedMember> members = q.getResultList();
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

    @SuppressWarnings("SqlResolve")
    public static Exceed getExceed(ExceedEnum ex) {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createQuery("SELECT COUNT(ex) FROM ExceedMember ex WHERE ex.exceed = :exceed");
        q.setParameter("exceed", ex.getName());

        Query points = em.createNativeQuery("SELECT e.points FROM shiro.\"GetCurrentExceedScores\" e WHERE e.exceed = :exceed");
        points.setParameter("exceed", ex.getName());

        int memberCount = ((Long) q.getSingleResult()).intValue();

        try {
            return new Exceed(ex, memberCount, ((BigDecimal) points.getSingleResult()).longValue());
        } finally {
            em.close();
        }
    }

    @SuppressWarnings({"SqlResolve", "unchecked"})
    public static ExceedEnum findWinner() {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createNativeQuery("SELECT e.exceed FROM shiro.\"GetCurrentExceedScores\" e");

        List<Object> ex = q.getResultList();
        em.close();

        Object winner = ex.get(0);

        return ExceedEnum.getByName(String.valueOf(winner));
    }

    public static void markWinner(ExceedEnum ex) {
        EntityManager em = Manager.getEntityManager();

        MonthWinner m = new MonthWinner();
        m.setExceed(ex.getName());

        saveScores();

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

    public static double getMemberShare(String id) {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createNativeQuery("""
                SELECT x.prcnt
                FROM (
                         SELECT em.id 
                              , em.contribution /
                                NULLIF((SELECT SUM(emi.contribution) FROM ExceedMember emi WHERE emi.exceed = em.exceed), 0) AS prcnt
                         FROM ExceedMember em
                         WHERE em.exceed <> ''
                     ) x
                WHERE x.prcnt IS NOT NULL
                AND x.id = :id
                """);
        q.setParameter("id", id);

        try {
            return ((BigDecimal) q.getSingleResult()).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
        } catch (NoResultException e) {
            return 0;
        } finally {
            em.close();
        }
    }

    @SuppressWarnings({"unchecked"})
    public static List<ExceedScore> getExceedHistory(ExceedEnum ex) {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createQuery("SELECT e FROM ExceedScore e WHERE e.exceed = :ex AND YEAR(e.timestamp) = YEAR(CURRENT_DATE)", ExceedScore.class);
        q.setParameter("ex", ex);

        try {
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public static void saveScores() {
        EntityManager em = Manager.getEntityManager();

        for (ExceedEnum ee : ExceedEnum.values()) {
            Exceed ex = getExceed(ee);

            em.getTransaction().begin();
            em.merge(new ExceedScore(ee, ex.getExp(), LocalDate.now()));
            em.getTransaction().commit();
        }

        em.close();
    }

    public static boolean verifyMonth() {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createQuery("SELECT es FROM ExceedScore es WHERE es.timestamp = :date");
        q.setParameter("date", LocalDate.now());

        try {
            return q.getResultList().isEmpty();
        } finally {
            em.close();
        }
    }
}
