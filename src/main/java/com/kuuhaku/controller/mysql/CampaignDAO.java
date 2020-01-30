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

import com.kuuhaku.handlers.games.rpg.world.World;
import com.kuuhaku.model.persistent.Campaign;
import com.kuuhaku.utils.ShiroInfo;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CampaignDAO {
	public static void saveCampaigns(Map<String, World> games) {
		EntityManager em = Manager.getEntityManager();

		List<String> g = games.values().stream().map(World::getAsJSON).collect(Collectors.toList());

		em.getTransaction().begin();
		g.forEach(em::merge);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, World> getCampaigns() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM Campaign g", Campaign.class);

		em.close();

		Map<String, World> g = new HashMap<>();
		List<Campaign> cps = q.getResultList();

		cps.forEach(c -> g.put(c.getId(), ShiroInfo.getJSONFactory().fromJson(c.getData(), World.class)));

		return g;
	}
}
