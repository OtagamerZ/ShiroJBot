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

-- DROP MATERIALIZED VIEW IF EXISTS v_senshi_roles;
CREATE MATERIALIZED VIEW IF NOT EXISTS shiro.v_senshi_roles AS
WITH placed_cards AS (
                     SELECT match_id
                          , side
                          , frontline_id
                          , backline_id
                     FROM history_slot
                     WHERE frontline_id IS NOT NULL
                        OR backline_id IS NOT NULL
                     ORDER BY match_id DESC
                     )
SELECT *
     , cast(iif(role = 'ATTACKER', x.front_winrate, x.back_winrate) * x.total_winrate AS DOUBLE PRECISION) AS meta_strength
FROM (
     SELECT id
          , front_uses
          , front_wins
          , coalesce(front_wins / nullif(front_uses, 0), 0)                                                 AS front_winrate
          , back_uses
          , back_wins
          , coalesce(back_wins / nullif(back_uses, 0), 0)                                                   AS back_winrate
          , total_uses
          , total_wins
          , coalesce(total_wins / nullif(total_uses, 0), 0)                                                 AS total_winrate
          , CASE TRUE
                WHEN x.front_uses > x.total_uses * 0.7 THEN 'ATTACKER'
                WHEN x.back_uses > x.total_uses * 0.7 THEN 'SUPPORT'
         END                                                                                                AS role
          , least(abs(pow(x.front_uses - x.back_uses, 2) / pow(greatest(x.back_uses, x.front_uses), 2)), 1) AS surety
     FROM (
          SELECT x.id
               , sum(x.front_uses)               AS front_uses
               , sum(x.front_wins)               AS front_wins
               , sum(x.back_uses)                AS back_uses
               , sum(x.back_wins)                AS back_wins
               , sum(x.front_uses + x.back_uses) AS total_uses
               , sum(x.front_wins + x.back_wins) AS total_wins
          FROM (
               SELECT x.id
                    , x.match_id
                    , x.side
                    , count(nullif(x.frontline, FALSE))                            AS front_uses
                    , count(nullif(x.frontline AND hi.winner = x.side, FALSE))     AS front_wins
                    , count(nullif(x.frontline, TRUE))                             AS back_uses
                    , count(nullif(NOT x.frontline AND hi.winner = x.side, FALSE)) AS back_wins
               FROM (
                    SELECT match_id, side, frontline_id AS id, TRUE AS frontline
                    FROM placed_cards
                    UNION ALL
                    SELECT match_id, side, backline_id AS id, FALSE AS frontline
                    FROM placed_cards
                    ) x
                        INNER JOIN history_info hi ON hi.match_id = x.match_id
               WHERE x.id IS NOT NULL
               GROUP BY x.id, x.match_id, x.side
               ) x
                   INNER JOIN card c ON c.id = x.id
          WHERE c.rarity NOT IN ('FUSION', 'NONE')
          GROUP BY x.id
          ORDER BY total_uses DESC
          ) x
     ) x;

CREATE UNIQUE INDEX IF NOT EXISTS senshi_roles_id ON v_senshi_roles (id);