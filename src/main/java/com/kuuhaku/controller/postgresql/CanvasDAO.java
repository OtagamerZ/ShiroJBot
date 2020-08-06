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

import com.kuuhaku.model.persistent.PixelCanvas;
import com.kuuhaku.model.persistent.PixelOperation;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class CanvasDAO {
	public static PixelCanvas getCanvas() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM PixelCanvas c WHERE c.shelved = false", PixelCanvas.class);
		q.setMaxResults(1);

		try {
			PixelCanvas p = (PixelCanvas) q.getSingleResult();
			em.close();

			return p;
		} catch (NoResultException e) {
			em.close();

			return new PixelCanvas();
		}
	}

	public static void saveCanvas(PixelCanvas canvas) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(canvas);
		em.getTransaction().commit();

		em.close();
	}

	public static void saveOperation(PixelOperation op) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(op);
		em.getTransaction().commit();

		em.close();
	}
}
