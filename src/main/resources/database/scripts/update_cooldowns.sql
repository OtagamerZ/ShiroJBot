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

CREATE OR REPLACE PROCEDURE update_cooldowns()
    LANGUAGE sql
AS
$$
UPDATE card_descriptions d
SET description = x.new
FROM (
         SELECT x.id
              , x.description
              , regexp_replace(x.description, '\d+(?=\{cd})', x.effect) AS new
         FROM (
                  SELECT cd.id
                       , cd.description
                       , (regexp_matches(cd.description, '\d+(?=\{cd})'))[1] AS current
                       , (regexp_matches(s.effect, '(?<=\.cooldown = )\d+'))[1] AS effect
                  FROM card_descriptions cd
                           INNER JOIN senshi s  ON s.card_id = cd.id
                  WHERE s.effect LIKE '%.cooldown = %'

              ) x
         WHERE x.current != x.effect
     ) x
WHERE d.id = x.id
$$;
