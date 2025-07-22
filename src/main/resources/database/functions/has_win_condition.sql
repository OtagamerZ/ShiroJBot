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

CREATE OR REPLACE FUNCTION shiro.has_win_condition(VARCHAR, VARCHAR)
    RETURNS BOOLEAN
    IMMUTABLE
    LANGUAGE sql
AS
$$
SELECT count(1) > 0
FROM user_matches($1)
WHERE winner = side
  AND win_condition = lower($2)
$$;

CREATE OR REPLACE FUNCTION shiro.has_win_condition_any(VARCHAR, VARCHAR)
    RETURNS BOOLEAN
    IMMUTABLE
    LANGUAGE sql
AS
$$
SELECT count(1) > 0
FROM user_matches($1)
WHERE win_condition = lower($2)
$$;