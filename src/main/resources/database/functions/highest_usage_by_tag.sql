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

CREATE OR REPLACE FUNCTION usage_by_tag(VARCHAR, VARCHAR[])
    RETURNS TABLE(id INT, total BIGINT)
    LANGUAGE plpgsql
AS
$body$
BEGIN
    RETURN QUERY
    SELECT x.id
         , count(1) AS used
    FROM (
         SELECT x.id
              , x.front || x.back AS cards
         FROM (
              SELECT x.id
                   , jsonb_path_query_array(x.placed, '$[*].frontline') AS front
                   , jsonb_path_query_array(x.placed, '$[*].backline')  AS back
              FROM (
                   SELECT w.id
                        , jsonb_path_query_array(w.turns, '$.placed') AS placed
                   FROM v_match_winner w
                   WHERE uid = $1
                   ) x
              ) x
         ) x
             CROSS JOIN jsonb_array_elements_text(x.cards) e(card)
    WHERE e.card IN (SELECT card_id FROM senshi WHERE tags \?& $2)
    GROUP BY x.id
    ORDER BY used DESC;
END;
$body$;

CREATE OR REPLACE FUNCTION usage_by_tag(VARCHAR, VARIADIC VARCHAR[])
    RETURNS TABLE
            (
                id    INT,
                total BIGINT
            )
    LANGUAGE plpgsql
AS
$body$
BEGIN
    RETURN QUERY
        SELECT x.id
             , count(1) AS used
        FROM (
                 SELECT x.id
                      , x.front || x.back AS cards
                 FROM (
                          SELECT x.id
                               , jsonb_path_query_array(x.placed, '$[*].frontline') AS front
                               , jsonb_path_query_array(x.placed, '$[*].backline')  AS back
                          FROM (
                                   SELECT w.id
                                        , jsonb_path_query_array(w.turns, '$.placed') AS placed
                                   FROM v_match_winner w
                                   WHERE uid = $1
                               ) x
                      ) x
             ) x
                 CROSS JOIN jsonb_array_elements_text(x.cards) e(card)
        WHERE e.card IN (
                            SELECT card_id
                            FROM senshi
                            WHERE tags
    \
    ?& $2)
    GROUP BY x.id
    ORDER BY used DESC;
END;
$body$;