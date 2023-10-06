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

CREATE OR REPLACE FUNCTION get_weight(VARCHAR)
    RETURNS INT
    IMMUTABLE
    LANGUAGE sql
AS
$$
SELECT CASE type
           WHEN 'KAWAIPON' THEN cast(425 * pow(1.3, tier) AS INT)
           WHEN 'EVOGEAR' THEN cast(45 * pow(2, tier) AS INT)
           WHEN 'FIELD' THEN 100
           END AS weight
FROM (
         SELECT c.id
              , 6 - coalesce(get_rarity_index(c.rarity), e.tier * 5 / 4)          AS tier
              , iif(get_rarity_index(c.rarity) IS NOT NULL, 'KAWAIPON', c.rarity) AS type
         FROM card c
                  LEFT JOIN evogear e ON c.id = e.card_id AND e.tier > 0
                  LEFT JOIN field f ON c.id = f.card_id
         WHERE C.rarity <> 'ULTIMATE'
           AND NOT coalesce(f.effect, FALSE)
     ) x
WHERE x.id = $1
$$;

CREATE OR REPLACE FUNCTION get_weight(VARCHAR, VARCHAR)
    RETURNS INT
    IMMUTABLE
    LANGUAGE sql
AS
$$
SELECT cast(round(get_weight($1) * iif(is_fav($2, $1), 1.5, 1.0)) AS INT)
$$;

CREATE OR REPLACE FUNCTION get_weight(VARCHAR, INT)
    RETURNS INT
    IMMUTABLE
    LANGUAGE sql
AS
$$
SELECT CASE type
           WHEN 'KAWAIPON' THEN cast(425 * pow(1.3, tier) AS INT)
           WHEN 'EVOGEAR' THEN cast(45 * pow(2, tier) AS INT)
           WHEN 'FIELD' THEN 100
           END AS weight
FROM (
         SELECT 6 - ($2 * 5 / 4) AS tier, $1 AS type
     ) x
$$;
