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
SELECT rank() OVER (ORDER BY x.winrate * x.match_count - x.penalty DESC) AS pos
     , x.uid
     , x.name
     , x.winrate
     , x.match_count
     , cast(x.winrate * x.match_count - x.penalty AS INT)                AS score
FROM (
     SELECT x.uid
          , x.name
          , round(x.wins * 100.0 / x.match_count, 2) AS winrate
          , x.match_count
          , x.penalty
     FROM (
          SELECT acc.uid
               , acc.name
               , count(nullif(hp.side = hi.winner, FALSE))                        AS wins
               , count(1)                                                         AS match_count
               , coalesce(cast(acc.inventory -> 'LEAVER_TICKET' AS INT), 0) * 250 AS penalty
          FROM history_info hi
                   INNER JOIN history_player hp ON hi.match_id = hp.match_id
                   INNER JOIN account acc ON hp.uid = acc.uid
                   INNER JOIN account_settings s ON acc.uid = s.uid
          WHERE hi.winner IS NOT NULL
            AND NOT s.private
          GROUP BY acc.uid, acc.name, penalty
          ) x
     ) x
WHERE x.winrate IS NOT NULL
ORDER BY pos