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
                                              SELECT data -> 'turns' -> 0 -> lower(head ->> 'winner') -> 'deck' AS deck
                                              FROM match_history
                                              WHERE has(head, 'winner')
                                                AND jsonb_array_length(data -> 'turns') > 10
                                              ORDER BY id
                                              LIMIT 30
                                              ) x
                                         )) x(card)
          GROUP BY x.card
          ) x
     ) x
WHERE x.number <= CASE (x.type)
                      WHEN 2 THEN 20
                      WHEN 4 THEN 6
                      WHEN 8 THEN 2
    END
ORDER BY freq DESC, card;