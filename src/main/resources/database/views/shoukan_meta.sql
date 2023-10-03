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

DROP VIEW IF EXISTS v_shoukan_meta;
CREATE OR REPLACE VIEW v_shoukan_meta AS
SELECT x.card AS card_id
     , x.freq
     , x.type
FROM (
     SELECT x.card
          , x.freq
          , x.type
          , row_number() OVER (PARTITION BY x.type ORDER BY x.freq DESC) AS number
     FROM (
          SELECT x.card
               , count(1)         AS freq
               , get_type(x.card) AS type
          FROM jsonb_array_elements_text((
                                         SELECT jsonb_merge(x.deck)
                                         FROM (
                                              SELECT turns -> 0 -> lower(info ->> 'winner') -> 'deck'   AS deck
                                                   , jsonb_array_length(turns)                          AS turns
                                                   , round(geo_mean(jsonb_array_length(turns)) OVER ()) AS turn_fac
                                              FROM match_history
                                              WHERE has(info, 'winner')
                                              ORDER BY id
                                              LIMIT 30
                                              ) x
                                         WHERE x.turns > x.turn_fac
                                         )) x(card)
          GROUP BY x.card
          ) x
     ) x
WHERE x.number <= CASE (x.type)
                      WHEN 3 THEN 20
                      WHEN 4 THEN 6
                      WHEN 8 THEN 2
    END
ORDER BY freq DESC, card;
