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

-- DROP VIEW IF EXISTS kawaipon.v_card_counter;
CREATE OR REPLACE VIEW kawaipon.v_card_counter AS
SELECT c.anime_id,
       count(1) AS count
FROM card c
         INNER JOIN anime a on a.id = c.anime_id
WHERE a.visible
  AND get_rarity_index(c.rarity) BETWEEN 1 AND 5
GROUP BY c.anime_id;
