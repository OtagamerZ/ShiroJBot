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

CREATE OR REPLACE FUNCTION get_type(VARCHAR)
    RETURNS INT
    LANGUAGE sql
AS
$$
SELECT cast((count(f) << 3) | (count(e) << 2) | ((count(s) | count(k)) << 1) AS INT)
FROM card c
         LEFT JOIN card k ON k.id = c.id AND get_rarity_index(k.rarity) BETWEEN 1 AND 5
         LEFT JOIN senshi s ON c.id = s.card_id
         LEFT JOIN evogear e ON c.id = e.card_id
         LEFT JOIN field f ON c.id = f.card_id
WHERE c.id = $1;
$$;