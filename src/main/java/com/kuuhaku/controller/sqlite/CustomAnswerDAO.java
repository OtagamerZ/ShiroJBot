package com.kuuhaku.controller.sqlite;

import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Guild;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class CustomAnswerDAO {
	@SuppressWarnings("unchecked")
	public static CustomAnswers getCAByTrigger(String trigger, String guild) {
		EntityManager em = Manager.getEntityManager();
		List<CustomAnswers> ca;

		Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE LOWER(gatilho) LIKE ?1 AND guildID = ?2", CustomAnswers.class);
		q.setParameter(1, trigger.toLowerCase());
		q.setParameter(2, guild);
		ca = (List<CustomAnswers>) q.getResultList();

		em.close();

		return ca.size() > 0 ? ca.get(Helper.rng(ca.size())) : null;
	}

	public static CustomAnswers getCAByID(Long id) {
		EntityManager em = Manager.getEntityManager();
		CustomAnswers ca;

		Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE id = ?1", CustomAnswers.class);
		q.setParameter(1, id);
		ca = (CustomAnswers) q.getSingleResult();

		em.close();

		return ca;
	}

	public static void addCAtoDB(Guild g, String trigger, String answer) {
		EntityManager em = Manager.getEntityManager();

		CustomAnswers ca = new CustomAnswers();
		ca.setGuildID(g.getId());
		ca.setGatilho(trigger);
		ca.setAnswer(answer);

		em.getTransaction().begin();
		em.merge(ca);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeCAFromDB(CustomAnswers ca) {
		EntityManager em = Manager.getEntityManager();

		ca.setMarkForDelete(true);

		em.getTransaction().begin();
		em.merge(ca);
		em.getTransaction().commit();

		em.close();
	}
}
