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

-- DROP VIEW IF EXISTS v_codex_progress;
CREATE OR REPLACE VIEW v_codex_progress AS
SELECT DISTINCT x.uid
              , x.races[1] AS race_1
              , x.races[2] AS race_2
              , race_flag(x.races[1]) | race_flag(x.races[2]) AS flag
              , x.variant
FROM (
     SELECT x.uid
          , x.variant
          , ARRAY(SELECT unnest(ARRAY [x.major, x.minor ->> 0]) ORDER BY 1) AS races
     FROM (
          SELECT w.uid
               , w.info -> 'origin' ->> 'major'                                    AS major
               , w.info -> 'origin' -> 'minor'                                     AS minor
               , coalesce(cast(w.info -> 'origin' -> 'variant' AS BOOLEAN), FALSE) AS variant
          FROM v_match_winner w
          ) x
     WHERE x.major NOT IN ('NONE', 'MIXED')
       AND jsonb_array_length(x.minor) > 0
     ) x
ORDER BY x.uid