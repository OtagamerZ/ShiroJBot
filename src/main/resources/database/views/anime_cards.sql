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

DROP VIEW IF EXISTS v_anime_cards;
CREATE OR REPLACE VIEW v_anime_cards AS
SELECT x.id
     , x.name
     , x.rarity
     , x.rarity_idx
     , x.anime_id
FROM (
     SELECT c.id
          , c.name
          , c.rarity
          , get_rarity_index(c.rarity) AS rarity_idx
          , c.anime_id
     FROM card c
              INNER JOIN anime a on a.id = c.anime_id
     WHERE a.visible
     ) x
WHERE x.rarity_idx BETWEEN 1 AND 5
ORDER BY x.rarity_idx DESC, x.id