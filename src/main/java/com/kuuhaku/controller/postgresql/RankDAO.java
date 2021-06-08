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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class RankDAO {

	@SuppressWarnings("unchecked")
	public static List<String> getLevelRanking(String guild) {
		EntityManager em = Manager.getEntityManager();

		Query q;
		if (guild == null) {
			q = em.createNativeQuery("""
					SELECT row_number() OVER (ORDER BY mb.xp DESC) || ' - ' || split_part(l.usr, '#', 1) || ' (' || gc.name || ')'
					FROM member mb
					         INNER JOIN (SELECT DISTINCT ON (uid) l.uid, l.usr
					                     FROM logs l
					                     WHERE l.uid <> '') l ON l.uid = mb.uid
					         INNER JOIN guildconfig gc ON gc.guildid = mb.sid
					WHERE is_blacklisted(mb.uid) IS NULL
					ORDER BY mb.xp DESC
					""", String.class);
		} else {
			q = em.createNativeQuery("""
					SELECT row_number() OVER (ORDER BY mb.xp DESC) || ' - ' || split_part(l.usr, '#', 1)
					FROM member mb
					         INNER JOIN (SELECT DISTINCT ON (uid) l.uid, l.usr
					                     FROM logs l
					                     WHERE l.uid <> '') l ON l.uid = mb.uid
					         INNER JOIN guildconfig gc ON gc.guildid = mb.sid
					WHERE gc.guildid = :guild
					  AND is_blacklisted(mb.uid) IS NULL
					ORDER BY mb.xp DESC
					""", String.class);
			q.setParameter("guild", guild);
		}

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getCreditRanking() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT row_number() OVER (ORDER BY a.balance DESC) || ' - ' || split_part(l.usr, '#', 1) || ' (' || to_char(a.balance, 'FM9,999,999,999') || ' CR)'
				FROM account a
				             INNER JOIN (SELECT DISTINCT ON (uid) l.uid, l.usr
				                         FROM logs l
				                         WHERE l.uid <> '') l ON l.uid = a.uid
				WHERE is_blacklisted(a.uid) IS NULL
				ORDER BY a.balance DESC
				""", String.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getCardRanking() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT row_number() OVER (ORDER BY kc.foil + kc.normal DESC, kc.foil DESC, kc.normal DESC) || ' - ' || split_part(l.usr, '#', 1) || ' (' || kc.foil ||' cromadas e ' || kc.normal || ' normais)'
				FROM kawaipon k
				             INNER JOIN (SELECT DISTINCT ON (uid) l.uid, l.usr
				                         FROM logs l
				                         WHERE l.uid <> '') l ON l.uid = k.uid
				             INNER JOIN (SELECT kc.kawaipon_id
				                              , count(1) FILTER (WHERE NOT kc.foil) AS normal
				                              , count(1) FILTER (WHERE kc.foil)     AS foil
				                         FROM kawaiponcard kc
				                         GROUP BY kc.kawaipon_id) kc on k.id = kc.kawaipon_id
				WHERE is_blacklisted(k.uid) IS NULL
				ORDER BY kc.foil + kc.normal DESC, kc.foil DESC, kc.normal DESC
				""", String.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getVoiceRanking(String guild) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT row_number() OVER (ORDER BY mb.xp DESC) || ' - ' || split_part(l.usr, '#', 1) || ' (' || to_duration(mb.voicetime) ||')'
				FROM member mb
				             INNER JOIN (SELECT DISTINCT ON (uid) l.uid, l.usr
				                         FROM logs l
				                         WHERE l.uid <> '') l ON l.uid = mb.uid
				             INNER JOIN guildconfig gc ON gc.guildid = mb.sid
				WHERE gc.guildid = :guild
				  AND is_blacklisted(mb.uid) IS NULL
				ORDER BY mb.xp DESC
				""", String.class);
		q.setParameter("guild", guild);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}
}
