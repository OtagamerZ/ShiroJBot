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

-- DROP MATERIALIZED VIEW IF EXISTS v_evogear_usage;
CREATE MATERIALIZED VIEW IF NOT EXISTS v_evogear_usage AS
WITH equipped_evo AS (
                     SELECT match_id
                          , side
                          , frontline_id
                          , jsonb_array_elements_text(frontline_equips) AS card_id
                     FROM history_slot
                     WHERE frontline_id IS NOT NULL
                     )
SELECT *
     , cast(x.winrate * x.total_winrate AS DOUBLE PRECISION) AS meta_strength
FROM (
     SELECT x.card_id AS id
          , x.target_id
          , x.uses
          , x.wins
          , x.winrate
          , x.total_uses
          , x.total_wins
          , cast(x.total_wins AS DOUBLE PRECISION) / x.total_uses AS total_winrate
     FROM (
          SELECT x.card_id
               , x.target_id
               , x.uses
               , x.wins
               , cast(x.wins AS DOUBLE PRECISION) / x.uses AS winrate
               , sum(x.uses) OVER (PARTITION BY x.card_id) AS total_uses
               , sum(x.wins) OVER (PARTITION BY x.card_id) AS total_wins
          FROM (
               SELECT x.card_id
                    , x.frontline_id              AS target_id
                    , count(1)                    AS uses
                    , count(nullif(x.won, FALSE)) AS wins
               FROM (
                    SELECT ee.match_id
                         , ee.side
                         , ee.card_id
                         , ee.frontline_id
                         , ee.side = hi.winner AS won
                    FROM equipped_evo ee
                             INNER JOIN history_info hi ON hi.match_id = ee.match_id
                    ) x
               GROUP BY x.card_id, x.frontline_id
               ) x
          ) x
     ) x
ORDER BY x.id, x.uses DESC, x.wins DESC;

CREATE UNIQUE INDEX IF NOT EXISTS evogear_usage_id ON v_evogear_usage (id, target_id);