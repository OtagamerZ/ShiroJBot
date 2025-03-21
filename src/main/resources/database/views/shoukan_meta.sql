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

-- DROP VIEW IF EXISTS v_shoukan_meta;
CREATE OR REPLACE VIEW v_shoukan_meta AS
SELECT x.card AS card_id
     , x.freq
     , x.type
FROM (
     SELECT x.card
          , x.freq
          , x.type
          , row_number() OVER (PARTITION BY x.type ORDER BY x.freq DESC) AS row
     FROM (
          SELECT x.card
               , count(1)         AS freq
               , get_type(x.card) AS type
          FROM jsonb_array_elements_text(
                       (
                       SELECT jsonb_merge(x.cards) AS cards
                       FROM (
                            SELECT x.cards
                                 , x.total_turns
                                 , round(geo_mean(x.total_turns) OVER ()) AS avg_turns
                            FROM (
                                 SELECT DISTINCT ON (hi.match_id) hs.hand || hs.deck                       AS cards
                                                                , count(1) OVER (PARTITION BY hi.match_id) AS total_turns
                                 FROM history_info hi
                                          INNER JOIN history_side hs ON hi.match_id = hs.match_id AND hi.winner = hs.side
                                 ORDER BY hi.match_id, hs.turn
                                 LIMIT 30
                                 ) x
                            ) x
                       WHERE x.total_turns >= x.avg_turns
                       )
               ) x(card)
          GROUP BY x.card
          ORDER BY freq DESC
          ) x
     ) x
WHERE x.row <= CASE (x.type)
                   WHEN 3 THEN 15
                   WHEN 4 THEN 6
                   WHEN 8 THEN 2
    END
ORDER BY x.type, x.freq DESC, x.card;