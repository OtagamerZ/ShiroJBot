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
                    SELECT (row_number() OVER () + 15 * :page) || x.v AS v
                    FROM (
                             SELECT *
                             FROM (
                                      SELECT ' - ' || "GetUsername"(mb.uid) || ' (Level ' || ceil(sqrt(mb.xp / 100)) || ')' AS v
                                      FROM member mb
                                               INNER JOIN guildconfig gc ON gc.guildid = mb.sid
                                      WHERE NOT EXISTS(SELECT b.uid FROM blacklist b WHERE b.uid = mb.uid)
                                      ORDER BY mb.xp DESC
                                  ) x
                             WHERE x.v IS NOT NULL
                             LIMIT 15 OFFSET 15 * :page
                         ) x
                    """);
        } else {
            q = em.createNativeQuery("""
                    SELECT (row_number() OVER () + 15 * :page) || x.v AS v
                    FROM (
                             SELECT *
                             FROM (
                                      SELECT ' - ' || "GetUsername"(mb.uid) || ' (Level ' || ceil(sqrt(mb.xp / 100)) || ')' AS v
                                      FROM member mb
                                               INNER JOIN guildconfig gc ON gc.guildid = mb.sid
                                      WHERE gc.guildid = :guild
                                        AND NOT EXISTS(SELECT b.uid FROM blacklist b WHERE b.uid = mb.uid)
                                      ORDER BY mb.xp DESC
                                  ) x
                             WHERE x.v IS NOT NULL
                             LIMIT 15 OFFSET 15 * :page
                         ) x
                    """);
            q.setParameter("guild", guild);
        }
        q.setParameter("page", page);

        try {
            return ((List<Object>) q.getResultList()).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> getCreditRanking(int page) {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createNativeQuery("""
                SELECT (row_number() OVER () + 15 * :page) || x.v AS v
                FROM (
                         SELECT *
                         FROM (
                                  SELECT ' - ' || "GetUsername"(a.uid) || ' (' || to_char(a.balance - a.loan, 'FM9,999,999,999') || ' CR)' AS v
                                  FROM account a
                                  WHERE NOT EXISTS(SELECT b.uid FROM blacklist b WHERE b.uid = a.uid)
                                  ORDER BY a.balance - a.loan DESC
                              ) x
                         WHERE x.v IS NOT NULL
                         LIMIT 15 OFFSET 15 * :page
                     ) x
                """);
        q.setParameter("page", page);

        try {
            return ((List<Object>) q.getResultList()).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> getCardRanking(int page) {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createNativeQuery("""
                SELECT (row_number() OVER () + 15 * :page) || x.v AS v
                FROM (
                         SELECT *
                         FROM (
                                  SELECT ' - ' || "GetUsername"(k.uid) || ' (' || kc.foil || ' cromadas e ' || kc.normal ||
                                         ' normais)' AS v
                                  FROM kawaipon k
                                           INNER JOIN (SELECT kc.kawaipon_id
                                                            , count(1) FILTER (WHERE kc.foil = FALSE) AS normal
                                                            , count(1) FILTER (WHERE kc.foil)         AS foil
                                                       FROM kawaiponcard kc
                                                       GROUP BY kc.kawaipon_id) kc on k.uid = kc.kawaipon_id
                                      AND NOT EXISTS(SELECT b.uid FROM blacklist b WHERE b.uid = k.uid)
                                  ORDER BY kc.foil + kc.normal DESC, kc.foil DESC, kc.normal DESC
                              ) x
                         WHERE x.v IS NOT NULL
                         LIMIT 15 OFFSET 15 * :page
                     ) x
                """);
        q.setParameter("page", page);

        try {
            return ((List<Object>) q.getResultList()).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> getVoiceRanking(String guild, int page) {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createNativeQuery("""
                SELECT (row_number() OVER () + 15 * :page) || x.v AS v
                FROM (
                         SELECT *
                         FROM (
                                  SELECT ' - ' || "GetUsername"(vt.uid) || ' (' || to_duration(vt.time) || ')' AS v
                                  FROM voicetime vt
                                  WHERE vt.sid = :guild
                                    AND NOT EXISTS(SELECT b.uid FROM blacklist b WHERE b.uid = vt.uid)
                                  ORDER BY vt.time DESC
                              ) x
                         WHERE x.v IS NOT NULL
                         LIMIT 15 OFFSET 15 * :page
                     ) x
                """);
        q.setParameter("guild", guild);
        q.setParameter("page", page);

        try {
            return ((List<Object>) q.getResultList()).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } finally {
            em.close();
        }
    }
}
