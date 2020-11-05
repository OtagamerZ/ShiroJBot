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

import com.kuuhaku.controller.postgresql.BlacklistDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class KGotchiDAO {
	@SuppressWarnings("unchecked")
	public static List<Kawaigotchi> getAllKawaigotchi() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT k FROM Kawaigotchi k", Kawaigotchi.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	public static Kawaigotchi getKawaigotchi(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT k FROM Kawaigotchi k WHERE k.userId = :id", Kawaigotchi.class);
		q.setParameter("id", id);

		try {
			return (Kawaigotchi) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static void saveKawaigotchi(Kawaigotchi k) {
		if (BlacklistDAO.isBlacklisted(k.getUserId())) return;
		EntityManager em = Manager.getEntityManager();

		k.setHealth(Helper.clamp(k.getHealth(), 0, 100));
		k.setHunger(Helper.clamp(k.getHunger(), 0, 100));
		k.setMood(Helper.clamp(k.getMood(), 0, 100));
		k.setEnergy(Helper.clamp(k.getEnergy(), 0, 100));

		em.getTransaction().begin();
		em.merge(k);
		em.getTransaction().commit();

		em.close();
	}

	public static void deleteKawaigotchi(Kawaigotchi k) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		Query q = em.createQuery("DELETE FROM Kawaigotchi k WHERE k.userId = :uid");
		q.setParameter("uid", k.getUserId());
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}
}
