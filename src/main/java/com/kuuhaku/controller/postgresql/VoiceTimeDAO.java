/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.persistent.VoiceTime;
import com.kuuhaku.model.persistent.id.CompositeMemberId;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class VoiceTimeDAO {

	public static VoiceTime getVoiceTime(String id, String guild) {
		EntityManager em = Manager.getEntityManager();

		try {
			return Helper.getOr(em.find(VoiceTime.class, new CompositeMemberId(id, guild)), new VoiceTime(id, guild));
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<VoiceTime> getAllVoiceTimes(String guild) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT vt FROM VoiceTime vt WHERE vt.sid = :guild", VoiceTime.class);
		q.setParameter("guild", guild);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static void saveVoiceTime(VoiceTime vt) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(vt);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeVoiceTime(VoiceTime vt) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(vt);
		em.getTransaction().commit();

		em.close();
	}
}
