/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

CREATE OR REPLACE VIEW v_shoukan_meta AS
WITH x AS (
          SELECT x.id
               , x.field
               , jsonb_array_elements(x.slots -> 'placed') AS slots
          FROM (
               SELECT h.id
                    , jsonb_array_elements(h.data -> 'turns') ->> 'field'                   AS field
                    , jsonb_array_elements(h.data -> 'turns') -> lower(h.head ->> 'winner') AS slots
               FROM match_history h
               WHERE head ? 'winner'
                 AND h.id >= (
                             SELECT id
                             FROM match_history
                             WHERE head ? 'winner'
                             ORDER BY id DESC
                             LIMIT 1
                             ) - 30
               ORDER BY h.id DESC
               ) x
          )

SELECT x.card
     , x.freq
FROM (
     SELECT x.card
          , x.freq
          , x.type
          , row_number() OVER (PARTITION BY x.type ORDER BY x.freq DESC) AS number
     FROM (
          SELECT x.card
               , count(1)         AS freq
               , get_type(x.card) AS type
          FROM (
               SELECT jsonb_array_elements_text(x.cards) AS card
               FROM (
                    SELECT x.equips || x.front || x.back || x.field AS cards
                    FROM (
                         SELECT x.id
                              , jsonb_merge(x.slots -> 'frontline')  AS front
                              , jsonb_merge(x.slots -> 'backline')   AS back
                              , jsonb_merge(x.slots -> 'equipments') AS equips
                              , jsonb_agg(x.field)                   AS field
                         FROM x
                         GROUP BY x.id
                         ) x
                    ) x
               ) x
          GROUP BY x.card
          ) x
     WHERE x.card <> 'DEFAULT'
     ) x
WHERE x.number <= CASE (x.type)
                      WHEN 2 THEN 20
                      WHEN 4 THEN 6
                      WHEN 8 THEN 2
    END
ORDER BY freq DESC, card;