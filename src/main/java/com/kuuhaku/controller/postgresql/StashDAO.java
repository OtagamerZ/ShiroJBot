/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Market;
import com.kuuhaku.model.persistent.Stash;
import com.kuuhaku.utils.XStringBuilder;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class StashDAO {
	@SuppressWarnings("unchecked")
	public static List<Stash> getCards(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT s FROM Stash s WHERE owner = :id", Stash.class);
		q.setParameter("id", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static int getRemainingSpace(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT a.cardStashCapacity - (SELECT COUNT(1) FROM Stash s WHERE s.owner = a.uid)
				FROM Account a
				WHERE a.uid = :id
				""");
		q.setParameter("id", id);

		try {
			return ((Number) q.getSingleResult()).intValue();
		} finally {
			em.close();
		}
	}

	public static Stash getCard(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(Stash.class, id);
		} finally {
			em.close();
		}
	}

	public static void saveCard(Stash card) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(card);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeCard(Stash card) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.createQuery("DELETE FROM Stash WHERE id = :id")
				.setParameter("id", card.getId())
				.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	public static int getTotalCards(User u, CommandLine args) {
		EntityManager em = Manager.getEntityManager();

		XStringBuilder sb = new XStringBuilder(
				"""
						SELECT COUNT(*)
						FROM Stash s
						JOIN s.card c
						JOIN c.anime a
						WHERE s.owner = '%s'
						""".formatted(u.getId())
		);

		Map<String, String> conditions = new HashMap<>() {{
			put("n", "AND c.id LIKE UPPER(:nome)");
			put("r", "AND c.rarity LIKE UPPER(:raridade)");
			put("a", "AND a.id LIKE UPPER(:anime)");
			put("c", "AND m.foil = TRUE");
			put("k", "AND m.type = 'KAWAIPON'");
			put("e", "AND m.type = 'EVOGEAR'");
			put("f", "AND m.type = 'FIELD'");
		}};

		Map<String, Function<String, Object>> parser = new HashMap<>() {{
			put("r", KawaiponRarity::getByName);
		}};

		List<Consumer<Query>> params = new ArrayList<>();

		for (Option op : args.getOptions()) {
			if (op.hasArg()) {
				String arg = op.getValue();
				if (arg != null) {
					sb.appendNewLine(conditions.get(op.getOpt()));
					params.add(q -> q.setParameter(
							op.getLongOpt(),
							parser.getOrDefault(op.getOpt(), s -> s).apply(arg)
					));
				}
			} else {
				sb.appendNewLine(conditions.get(op.getOpt()));
			}
		}

		Query q = em.createQuery(sb.toString(), Number.class);
		for (Consumer<Query> param : params) {
			param.accept(q);
		}

		try {
			return ((Number) q.getSingleResult()).intValue();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Stash> getStashedCards(int page, User u, CommandLine args) {
		EntityManager em = Manager.getEntityManager();

		XStringBuilder sb = new XStringBuilder(
				"""
						SELECT s
						FROM Stash s
						JOIN s.card c
						JOIN c.anime a
						WHERE s.owner = '%s'
						""".formatted(u.getId())
		);

		Map<String, String> conditions = new HashMap<>() {{
			put("n", "AND m.card_id LIKE UPPER(:nome)");
			put("r", "AND c.rarity LIKE UPPER(:raridade)");
			put("a", "AND a.id LIKE UPPER(:anime)");
			put("c", "AND m.foil = TRUE");
			put("k", "AND m.type = 'KAWAIPON'");
			put("e", "AND m.type = 'EVOGEAR'");
			put("f", "AND m.type = 'FIELD'");
		}};

		Map<String, Function<String, Object>> parser = new HashMap<>() {{
			put("r", KawaiponRarity::getByName);
		}};

		List<Consumer<Query>> params = new ArrayList<>();

		for (Option op : args.getOptions()) {
			if (op.hasArg()) {
				String arg = op.getValue();
				if (arg != null) {
					sb.appendNewLine(conditions.get(op.getOpt()));
					params.add(q -> q.setParameter(
							op.getLongOpt(),
							parser.getOrDefault(op.getOpt(), s -> s).apply(arg)
					));
				}
			} else {
				sb.appendNewLine(conditions.get(op.getOpt()));
			}
		}

		sb.appendNewLine("ORDER BY m.price, m.foil DESC, c.rarity DESC, a.id, c.id");
		Query q = em.createQuery(sb.toString(), Market.class);
		if (page > -1) {
			q.setFirstResult(6 * page);
			q.setMaxResults(6);
		}

		for (Consumer<Query> param : params) {
			param.accept(q);
		}

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}
}

