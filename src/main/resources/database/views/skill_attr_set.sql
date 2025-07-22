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

-- DROP VIEW IF EXISTS dunhun.v_skill_attr_set;
CREATE OR REPLACE VIEW dunhun.v_skill_attr_set AS
SELECT id
     , req_race
     , req_tags
     , str
     , dex
     , wis
     , vit
     , (str + dex + wis + vit) AS total
     , x.class
FROM (
     SELECT id
          , req_race
          , req_tags
          , bit_get(req_attributes, 8, 0) AS str
          , bit_get(req_attributes, 8, 1) AS dex
          , bit_get(req_attributes, 8, 2) AS wis
          , bit_get(req_attributes, 8, 3) AS vit
          , get_attribute_class(req_attributes) AS class
     FROM skill
     WHERE req_attributes != -1
     ) x
ORDER BY -str, -(str + dex) / 2, -dex, -(dex + wis) / 2, -wis, -(wis + vit) / 2, -vit, -(vit + str) / 2, id