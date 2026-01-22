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

-- DROP VIEW IF EXISTS dunhun.v_basetype_attr_set;
CREATE OR REPLACE VIEW dunhun.v_basetype_attr_set AS
SELECT id
     , implicit_id
     , req_level
     , str / (10.0 * tier) AS str
     , dex / (10.0 * tier) AS dex
     , wis / (10.0 * tier) AS wis
     , vit / (10.0 * tier) AS vit
     , tier
FROM (SELECT id
           , gear_type
           , implicit_id
           , req_level
           , bit_get(req_attributes, 8, 0) AS str
           , bit_get(req_attributes, 8, 1) AS dex
           , bit_get(req_attributes, 8, 2) AS wis
           , bit_get(req_attributes, 8, 3) AS vit
           , (req_level - 1) / 21 + 1      AS tier
      FROM basetype
      WHERE regexp_like(id, '_[0-9]+$')) x
ORDER BY gear_type, cast(split_part(id, '_', -1) AS INT);