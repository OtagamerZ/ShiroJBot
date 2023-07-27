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

DROP VIEW IF EXISTS v_unused_archetypes;
CREATE OR REPLACE VIEW v_unused_archetypes AS
SELECT x.anime_id
     , x.cards
FROM (
         SELECT c.anime_id
              , count(1) OVER (PARTITION BY c.anime_id) AS cards
         FROM senshi s
                  INNER JOIN card c on c.id = s.card_id
                  LEFT JOIN archetype a ON a.id = c.anime_id
         WHERE a.id IS NULL
     ) x
WHERE x.cards >= 20 / 3
GROUP BY x.anime_id, x.cards
ORDER BY x.anime_id