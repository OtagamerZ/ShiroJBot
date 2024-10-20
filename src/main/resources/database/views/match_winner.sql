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

-- DROP VIEW IF EXISTS v_match_winner;
CREATE OR REPLACE VIEW v_match_winner AS
SELECT x.id
     , x.info ->> 'uid' AS uid
     , x.info
     , x.turns
     , x.match_info
FROM (
     SELECT x.id
          , x.info
          , jsonb_path_query_array(x.data, cast('$.' || x.winner AS JSONPATH)) AS turns
          , x.match_info
     FROM (
          SELECT x.id
               , x.info -> x.winner                                                                                               AS info
               , jsonb_path_query_array(x.turns,cast('$[*] ? (@.turn % 2 == ' || iif(x.winner = 'top', 0, 1) || ')' AS JSONPATH)) AS data
               , x.winner
               , (x.info - 'top' - 'bottom')                                                                                      AS match_info
          FROM (
               SELECT id
                    , info
                    , turns
                    , lower(info ->> 'winner') AS winner
               FROM match_history
               WHERE has(info, 'winner')
                 AND (info ->> 'winCondition') <> 'wo'
               ) x
          ) x
     ) x
WHERE x.info IS NOT NULL
ORDER BY x.id DESC
