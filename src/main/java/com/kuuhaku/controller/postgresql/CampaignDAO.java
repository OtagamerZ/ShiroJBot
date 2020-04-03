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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.Main;
import com.kuuhaku.handlers.games.rpg.deserializers.ItemDeserializer;
import com.kuuhaku.handlers.games.rpg.entities.Item;
import com.kuuhaku.handlers.games.rpg.world.World;
import com.kuuhaku.model.persistent.Campaign;
import com.kuuhaku.utils.ShiroInfo;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampaignDAO {
	public static void saveCampaigns(Map<String, World> games) {
		EntityManager em = Manager.getEntityManager();

		List<Campaign> c = new ArrayList<>();
		games.forEach((k, v) -> {
			Campaign cp = new Campaign();

			cp.setId(k);
			cp.setServer(Main.getInfo().getGuildByID(k).getName());
			cp.setData(v.getAsJSON());

			c.add(cp);
		});

		em.getTransaction().begin();
		c.forEach(em::merge);
		em.getTransaction().commit();

		em.close();
	}

	public static void closeCampaign(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM Campaign g WHERE id LIKE :id", Campaign.class);
		q.setParameter("id", id);

		try {
			Campaign c = (Campaign) q.getSingleResult();
			c.close();

			em.getTransaction().begin();
			em.merge(c);
			em.getTransaction().commit();
		} catch (NoResultException ignore) {
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, World> getCampaigns() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM Campaign g WHERE closed = FALSE", Campaign.class);

		Map<String, World> g = new HashMap<>();
		List<Campaign> cps = q.getResultList();

		cps.forEach(c -> g.put(c.getId(), ShiroInfo.getJSONFactory()
				.registerTypeAdapter(Item.class, new ItemDeserializer())
				.create().fromJson(c.getData(), World.class)));

		em.close();

		return g;
	}
}
