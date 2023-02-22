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

CREATE OR REPLACE VIEW v_match_loser AS
SELECT x.id
     , x.info ->> 'uid' AS uid
     , x.info           AS head
     , x.data
FROM (
     SELECT x.id
          , x.head -> x.loser                                               AS info
          , jsonb_path_query_array(data, CAST('$.' || x.loser AS JSONPATH)) AS data
     FROM (
          SELECT id
               , head
               , data
               , iif((head ->> 'winner') = 'TOP', VARCHAR 'bottom', VARCHAR 'top') AS loser
          FROM match_history
          WHERE has(head, 'winner')
          ) x
     ) x
WHERE x.info IS NOT NULL
ORDER BY x.id DESC