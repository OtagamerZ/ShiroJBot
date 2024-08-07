/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

-- DROP VIEW IF EXISTS v_shoukan_ranking;
CREATE OR REPLACE VIEW v_shoukan_ranking AS
SELECT rank() OVER (ORDER BY x.winrate * x.match_count DESC) AS pos
     , x.uid
     , x.name
     , x.winrate
     , x.match_count
     , cast(x.winrate * x.match_count AS INT)                AS score
FROM (
     SELECT x.uid
          , x.name
          , x.match_count
          , round(x.wins * 100.0 / x.match_count, 2) AS winrate
     FROM (
          SELECT x.uid
               , x.name
               , sum(iif(x.winner = x.uid, 1, NULL)) AS wins
               , x.match_count
          FROM (
               SELECT acc.uid
                    , acc.name
                    , h.info -> lower(h.info ->> 'winner') ->> 'uid' AS winner
                    , count(1) OVER (PARTITION BY acc.uid)           AS match_count
               FROM account acc
                        INNER JOIN match_history h ON acc.uid IN (h.info -> 'top' ->> 'uid', h.info -> 'bottom' ->> 'uid')
               ) x
          GROUP BY x.uid, x.name, x.match_count
          ) x
     ) x
WHERE x.winrate IS NOT NULL
ORDER BY pos
