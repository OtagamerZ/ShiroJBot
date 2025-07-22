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

CREATE OR REPLACE FUNCTION shiro.codex_progress(VARCHAR)
    RETURNS TABLE(uid VARCHAR, race_1 VARCHAR, race_2 VARCHAR, flag BIT, variant BOOLEAN)
    LANGUAGE sql
AS
$$
SELECT DISTINCT x.uid
              , x.races[1]                                    AS race_1
              , x.races[2]                                    AS race_2
              , race_flag(x.races[1]) | race_flag(x.races[2]) AS flag
              , x.variant
FROM (
     SELECT x.uid
          , x.variant
          , ARRAY(SELECT unnest(ARRAY [x.major, x.minor ->> 0]) ORDER BY 1) AS races
     FROM (
          SELECT um.uid
               , hp.major_race    AS major
               , hp.minor_races   AS minor
               , hp.using_variant AS variant
          FROM user_matches($1) um
                   INNER JOIN history_player hp ON um.match_id = hp.match_id AND um.uid = hp.uid
          ) x
     WHERE x.major NOT IN ('NONE', 'MIXED')
       AND jsonb_array_length(x.minor) > 0
     ) x
ORDER BY flag, x.variant
$$;