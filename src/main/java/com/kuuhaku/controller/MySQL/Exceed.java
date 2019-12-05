package com.kuuhaku.controller.MySQL;

import com.kuuhaku.model.Member;
import com.kuuhaku.model.MonthWinner;
import com.kuuhaku.utils.ExceedEnums;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.time.LocalDate;
import java.util.List;

public class Exceed {
	@SuppressWarnings("unchecked")
    public static List<com.kuuhaku.model.Member> getExceedMembers(ExceedEnums ex) {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createQuery("SELECT m FROM Member m WHERE exceed LIKE ?1", com.kuuhaku.model.Member.class);
        q.setParameter(1, ex.getName());

        List<com.kuuhaku.model.Member> members = (List<Member>) q.getResultList();
        em.close();

        return members;
    }

	@SuppressWarnings("unchecked")
    public static com.kuuhaku.model.Exceed getExceed(ExceedEnums ex) {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createQuery("SELECT m FROM Member m WHERE exceed LIKE ?1", Member.class);
        q.setParameter(1, ex.getName());

        List<Member> members = (List<Member>) q.getResultList();
        em.close();

        return new com.kuuhaku.model.Exceed(ex, members.size(), members.stream().mapToLong(Member::getXp).sum());
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
