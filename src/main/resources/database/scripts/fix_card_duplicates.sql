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
INSERT INTO stashed_card (type, card_id, deck_id, kawaipon_uid, price, uuid, trash, account_bound)
SELECT 'KAWAIPON',
       x.card_id,
       null,
       x.kawaipon_uid,
       0,
       x.uuid,
       false,
       false
FROM (
         SELECT kc.kawaipon_uid
              , kc.card_id
              , kc.uuid
              , row_number() over (PARTITION BY kc.kawaipon_uid, kc.card_id) AS row
         FROM kawaipon_card kc
                  LEFT JOIN stashed_card sc ON sc.uuid = kc.uuid
         WHERE sc.id IS NULL
     ) x
WHERE x.row > 1
$$;