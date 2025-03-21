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

CREATE OR REPLACE FUNCTION card_winrate(VARCHAR)
    RETURNS NUMERIC
    IMMUTABLE
    LANGUAGE sql
AS
$$
SELECT round(count(nullif(x.won, false)) * 100.0 / count(1), 2)
FROM (
     SELECT hi.winner
          , hs.side = hi.winner AS won
     FROM history_info hi
              INNER JOIN history_turn ht ON hi.match_id = ht.match_id
              INNER JOIN history_side hs ON hi.match_id = hs.match_id AND ht.turn = hs.turn
     WHERE ht.turn = 0
       AND (has(hs.hand, $1) OR has(hs.deck, $1))
     ) x
$$;
