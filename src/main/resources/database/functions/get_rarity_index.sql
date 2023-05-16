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

CREATE OR REPLACE FUNCTION get_rarity_index(VARCHAR)
    RETURNS INT
    IMMUTABLE
    LANGUAGE sql
AS
$$
SELECT CASE $1
           WHEN 'COMMON' THEN 1
           WHEN 'UNCOMMON' THEN 2
           WHEN 'RARE' THEN 3
           WHEN 'EPIC' THEN 4
           WHEN 'LEGENDARY' THEN 5
           WHEN 'ULTIMATE' THEN 6
           END;
$$;