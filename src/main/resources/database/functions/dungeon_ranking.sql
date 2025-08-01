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

CREATE OR REPLACE FUNCTION dunhun.dungeon_ranking(VARCHAR)
    RETURNS TABLE
            (
                hero_id  VARCHAR,
                floor    INT,
                sublevel INT,
                rank     INT
            )
    LANGUAGE sql
AS
$body$
SELECT x.hero_id
     , x.floor
     , x.sublevel
     , rank() OVER (ORDER BY x.floor DESC, x.sublevel DESC)
FROM (
     SELECT r.hero_id
          , r.floor
          , r.sublevel
          , count(1) AS party_size
     FROM dungeon_run r
              INNER JOIN dungeon_run_player rp ON rp.dungeon_id = r.dungeon_id
     WHERE r.dungeon_id = $1
     GROUP BY r.hero_id, r.floor, r.sublevel
     ) x
WHERE x.party_size = 1
ORDER BY x.floor DESC, x.sublevel DESC
LIMIT 10
$body$;