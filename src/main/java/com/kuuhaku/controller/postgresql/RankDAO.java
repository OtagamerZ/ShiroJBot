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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.stream.Collectors;

public class RankDAO {

	@SuppressWarnings("unchecked")
	public static List<String> getLevelRanking(String guild, int page) {
		EntityManager em = Manager.getEntityManager();

		Query q;
		if (guild == null) {
			q = em.createNativeQuery("""
					SELECT x.v
					     , CAST(split_part(x.v, ' - ', 1) AS INT) AS index
					FROM (
						SELECT row_number() OVER (ORDER BY mb.xp DESC) || ' - ' || u.name || ' (Level ' || mb.level || ' - ' || gc.name || ')' AS v
						FROM member mb
						INNER JOIN "GetUsername"(mb.uid) u ON u.uid = mb.uid
						INNER JOIN guildconfig gc ON gc.guildid = mb.sid
						WHERE NOT EXISTS (SELECT b.uid FROM blacklist b WHERE b.uid = mb.uid)
					) x
					ORDER BY index
					LIMIT 15 OFFSET 15 * :page
					""");
		} else {
			q = em.createNativeQuery("""
					SELECT x.v
					     , CAST(split_part(x.v, ' - ', 1) AS INT) AS index
					FROM (
						SELECT row_number() OVER (ORDER BY mb.xp DESC) || ' - ' || u.name || ' (Level ' || mb.level || ')' AS v
						FROM member mb
						INNER JOIN "GetUsername"(mb.uid) u ON u.uid = mb.uid
						INNER JOIN guildconfig gc ON gc.guildid = mb.sid
						WHERE gc.guildid = :guild
						AND NOT EXISTS (SELECT b.uid FROM blacklist b WHERE b.uid = mb.uid)
					) x
					ORDER BY index
					LIMIT 15 OFFSET 15 * :page
					""");
			q.setParameter("guild", guild);
		}
		q.setParameter("page", page);

		try {
			return ((List<Object[]>) q.getResultList())
					.stream().map(o -> (String) o[0])
					.collect(Collectors.toList());
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getCreditRanking(int page) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT x.v
				     , CAST(split_part(x.v, ' - ', 1) AS INT) AS index
				FROM (
					SELECT row_number() OVER (ORDER BY a.balance DESC) || ' - ' || u.name || ' (' || to_char(a.balance, 'FM9,999,999,999') || ' CR)' AS v
					FROM account a
					INNER JOIN "GetUsername"(a.uid) u ON u.uid = a.uid
					AND NOT EXISTS (SELECT b.uid FROM blacklist b WHERE b.uid = a.uid)
				) x
				ORDER BY index
				LIMIT 15 OFFSET 15 * :page
				""");
		q.setParameter("page", page);

		try {
			return ((List<Object[]>) q.getResultList())
					.stream().map(o -> (String) o[0])
					.collect(Collectors.toList());
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getCardRanking(int page) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT x.v
				     , CAST(split_part(x.v, ' - ', 1) AS INT) AS index
				FROM (
					SELECT row_number() OVER (ORDER BY kc.foil + kc.normal DESC, kc.foil DESC, kc.normal DESC) || ' - ' || u.name || ' (' || kc.foil ||' cromadas e ' || kc.normal || ' normais)' AS v
					FROM kawaipon k
					INNER JOIN "GetUsername"(k.uid) u ON u.uid = k.uid
					INNER JOIN (SELECT kc.kawaipon_id
					                 , count(1) FILTER (WHERE kc.foil = FALSE) AS normal
					                 , count(1) FILTER (WHERE kc.foil)     AS foil
					            FROM kawaiponcard kc
					            GROUP BY kc.kawaipon_id) kc on k.uid = kc.kawaipon_id
					AND NOT EXISTS (SELECT b.uid FROM blacklist b WHERE b.uid = k.uid)
				) x
				ORDER BY index
				LIMIT 15 OFFSET 15 * :page
				""");
		q.setParameter("page", page);

		try {
			return ((List<Object[]>) q.getResultList())
					.stream().map(o -> (String) o[0])
					.collect(Collectors.toList());
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getVoiceRanking(String guild, int page) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT x.v
				     , CAST(split_part(x.v, ' - ', 1) AS INT) AS index
				FROM (
				     SELECT row_number() OVER (ORDER BY vt.time DESC) || ' - ' || u.name || ' (' || to_duration(vt.time) || ')' AS v
				     FROM voicetime vt
				     INNER JOIN "GetUsername"(vt.uid) u ON u.uid = vt.uid
				     WHERE vt.sid = :guild
				     AND NOT EXISTS (SELECT b.uid FROM blacklist b WHERE b.uid = vt.uid)
				) x
				ORDER BY index
				LIMIT 15 OFFSET 15 * :page
				""");
		q.setParameter("guild", guild);
		q.setParameter("page", page);

		try {
			return ((List<Object[]>) q.getResultList())
					.stream().map(o -> (String) o[0])
					.collect(Collectors.toList());
		} finally {
			em.close();
		}
	}
}
