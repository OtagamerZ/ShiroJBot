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

-- DROP MATERIALIZED VIEW IF EXISTS v_balance;
CREATE MATERIALIZED VIEW IF NOT EXISTS v_balance AS
WITH ranking AS (
                SELECT uid, balance
                FROM account
                WHERE balance > 0
                )
SELECT last_value(r.balance) OVER w                AS lowest
     , last_value(r.uid) OVER w                    AS lowest_uid
     , first_value(r.balance) OVER w               AS highest
     , first_value(r.uid) OVER w                   AS highest_uid
     , cast(geo_mean(r.balance) OVER w AS INTEGER) AS avg
FROM ranking r
WINDOW w AS ()
LIMIT 1;