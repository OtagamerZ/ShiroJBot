/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

CREATE OR REPLACE VIEW v_match_winner AS
SELECT x.info ->> 'uid'  AS uid
     , x.info            AS head
     , x.data
FROM (
     SELECT head -> lower(head ->> 'winner')          AS info
          , jsonb_agg(it -> lower(head ->> 'winner')) AS data
     FROM match_history
              CROSS JOIN jsonb_array_elements(data) AS dt(it)
     GROUP BY id, info
     ) x
WHERE x.info IS NOT NULL