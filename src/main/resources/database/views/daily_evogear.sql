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

-- DROP VIEW IF EXISTS v_daily_evogear;
CREATE OR REPLACE VIEW v_daily_evogear AS
SELECT cast(setseed(extract(DOY FROM current_date) / 365) AS VARCHAR) AS card_id
UNION ALL
SELECT x.card_id
FROM (
     SELECT x.card_id
          , sum(iif(tier >= 4, 1, 0)) OVER (ROWS UNBOUNDED PRECEDING) AS tier_4
     FROM (
          SELECT card_id
               , tier
          FROM evogear
          WHERE tier > 0
          ORDER BY hashtextextended(card_id, get_seed())
          ) x
     ) x
   , generate_series(1, least(cast(random() * 4 AS INT), 3)) s
WHERE x.tier_4 < 2
OFFSET 1 LIMIT 10;