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

CREATE OR REPLACE FUNCTION card_cost(VARCHAR)
    RETURNS TABLE(id VARCHAR, mp_cost INT, hp_cost INT, sc_cost INT)
    LANGUAGE sql
AS
$$
SELECT c.id
     , COALESCE(s.mana, e.mana)
     , COALESCE(s.blood, e.blood)
     , COALESCE(s.sacrifices, e.sacrifices)
FROM card c
         LEFT JOIN senshi s ON s.card_id = c.id
         LEFT JOIN evogear e ON e.card_id = c.id
         LEFT JOIN field f ON f.card_id = c.id
WHERE c.id = $1
$$;