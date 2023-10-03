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

-- DROP VIEW IF EXISTS v_shoukan_ranking;
CREATE OR REPLACE VIEW v_shoukan_ranking AS
SELECT rank() OVER (ORDER BY x.winrate * x.match_count DESC) AS pos
     , x.uid
     , x.name
     , x.winrate
     , x.match_count
FROM (
         SELECT a.uid
              , a.name
              , user_winrate(a.uid) AS winrate
              , count(1)            AS match_count
         FROM account a
                  INNER JOIN v_matches m ON has(m.players, a.uid)
         GROUP BY a.uid, a.name, winrate
     ) x
WHERE x.winrate IS NOT NULL
ORDER BY pos
