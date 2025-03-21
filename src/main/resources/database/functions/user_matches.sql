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

CREATE OR REPLACE FUNCTION user_matches(VARCHAR)
    RETURNS TABLE
            (
                match_id INT,
                uid VARCHAR,
                arcade VARCHAR,
                seed BIGINT,
                match_timestamp TIMESTAMP WITH TIME ZONE,
                win_condition VARCHAR,
                winner VARCHAR,
                bottom_id VARCHAR,
                top_id VARCHAR,
                side VARCHAR
            )
    LANGUAGE sql
AS
$$
SELECT match_id
     , $1
     , arcade
     , seed
     , match_timestamp
     , win_condition
     , winner
     , bottom_id
     , top_id
     , iif(top_id = $1, cast('TOP' AS VARCHAR), cast('BOTTOM' AS VARCHAR))
FROM history_info
WHERE winner IS NOT NULL
  AND $1 IN (top_id, bottom_id)
$$;
