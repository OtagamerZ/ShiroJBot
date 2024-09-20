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

-- DROP VIEW IF EXISTS v_collection_counter;
CREATE OR REPLACE VIEW v_collection_counter AS
SELECT kc.kawaipon_uid                       AS uid
     , c.anime_id
     , count(1) FILTER (WHERE NOT cd.chrome) AS normal
     , count(1) FILTER (WHERE cd.chrome)     AS chrome
FROM kawaipon.kawaipon_card kc
         INNER JOIN kawaipon.card_details cd ON cd.card_uuid = kc.uuid
         INNER JOIN kawaipon.card c ON c.id = kc.card_id
         LEFT JOIN kawaipon.stashed_card sc ON kc.uuid = sc.uuid
WHERE sc.id IS NULL
GROUP BY kc.kawaipon_uid, c.anime_id;
