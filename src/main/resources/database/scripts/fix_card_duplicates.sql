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

CREATE OR REPLACE PROCEDURE fix_card_duplicates()
    LANGUAGE sql
AS
$$
UPDATE stashed_card sc
    SET in_collection = FALSE
    FROM (
         SELECT 'KAWAIPON',
             x.card_id,
             null,
             x.kawaipon_uid,
             0,
             x.uuid,
             false,
             false
         FROM (
              SELECT sc.kawaipon_uid
                   , sc.card_id
                   , sc.uuid
                   , row_number() over (PARTITION BY sc.kawaipon_uid, sc.card_id) AS row
              FROM stashed_card sc
              WHERE sc.in_collection
              ) x
         WHERE x.row > 1
         ) x
    WHERE x.uuid = sc.uuid;
$$;
