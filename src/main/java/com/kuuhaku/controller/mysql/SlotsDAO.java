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

import com.kuuhaku.model.persistent.Slots;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

public class SlotsDAO {
	public static Slots getSlots() {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(Slots.class, 1);
		} catch (NoResultException e) {
			return saveSlots(new Slots());
		} finally {
			em.close();
		}
	}

	public static Slots saveSlots(Slots s) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(s);
		em.getTransaction().commit();

		em.close();

		return s;
	}
}
