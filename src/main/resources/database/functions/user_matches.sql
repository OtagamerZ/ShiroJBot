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
    RETURNS TABLE(id INT, info JSONB, turns JSONB, side VARCHAR)
    LANGUAGE sql
AS
$$
SELECT mh.id
     , mh.info
     , mh.turns
     , iif((mh.info -> 'top' ->> 'uid') = $1, cast('top' AS VARCHAR), cast('bottom' AS VARCHAR))
FROM match_history mh
WHERE $1 IN (mh.info -> 'top' ->> 'uid', mh.info -> 'bottom' ->> 'uid')
$$;
