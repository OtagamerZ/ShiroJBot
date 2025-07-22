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

-- DROP VIEW IF EXISTS dunhun.v_global_drops;
CREATE OR REPLACE VIEW dunhun.v_global_drops AS
SELECT id
     , weight
FROM jsonb_to_recordset('[
  {"id": "NULLIFYING_POWDER", "weight": 8000},
  {"id": "ALTERATING_EMBER", "weight": 5500},
  {"id": "UNSTABLE_DICE", "weight": 2000},
  {"id": "UNMAKING_HAMMER", "weight": 1800},
  {"id": "ALCHEMICAL_EMBER", "weight": 1200},
  {"id": "INTRINSIC_DICE", "weight": 1200},
  {"id": "CHAOTIC_EMBER", "weight": 1000}
]') AS (id VARCHAR, weight INT)