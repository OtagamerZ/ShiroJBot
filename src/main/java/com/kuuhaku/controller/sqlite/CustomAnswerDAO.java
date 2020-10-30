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

import com.kuuhaku.model.persistent.CustomAnswers;
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

		Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE LOWER(gatilho) = ?1 AND guildID = ?2 AND markForDelete = FALSE", CustomAnswers.class);
		q.setParameter(1, trigger.toLowerCase());
		q.setParameter(2, guild);
		ca = q.getResultList();

		em.close();

		return ca.size() > 0 ? ca.get(Helper.rng(ca.size(), true)) : null;
	}

	public static CustomAnswers getCAByID(Long id) {
		EntityManager em = Manager.getEntityManager();
		CustomAnswers ca;

		Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE id = ?1 AND markForDelete = FALSE", CustomAnswers.class);
		q.setParameter(1, id);
		ca = (CustomAnswers) q.getSingleResult();

		em.close();

		return ca;
	}

	@SuppressWarnings("unchecked")
	public static List<CustomAnswers> getCAByGuild(String id) {
		EntityManager em = Manager.getEntityManager();
		CustomAnswers ca;

		Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE guildID = ?1 AND markForDelete = FALSE", CustomAnswers.class);
		q.setParameter(1, id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
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
