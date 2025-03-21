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

CREATE OR REPLACE FUNCTION usage_by_tag(VARCHAR, VARCHAR[])
    RETURNS TABLE(id INT, total BIGINT)
    LANGUAGE sql
AS
$$
SELECT um.match_id
     , count(1) AS used
FROM user_matches($1) um
         INNER JOIN history_slot hs ON um.match_id = hs.match_id AND um.side = hs.side
         INNER JOIN senshi s ON s.card_id IN (hs.frontline_id, hs.backline_id)
WHERE um.winner = um.side
  AND s.tags \?& $2
GROUP BY um.match_id
ORDER BY used DESC
$$;