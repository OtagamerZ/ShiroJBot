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

CREATE OR REPLACE FUNCTION get_missing_deck_references(VARCHAR)
    RETURNS TABLE (id INT, card_id VARCHAR, index INT, deck_copy INT, stash_copy INT)
    LANGUAGE plpgsql
AS
$body$
BEGIN
    IF ($1 NOT IN ('senshi', 'evogear', 'field')) THEN
        RAISE EXCEPTION 'Parameter must be senshi, evogear or field';
    END IF;

    RETURN QUERY EXECUTE format($$
    SELECT d.id
        , dx.card_id
        , dx.index
        ,  CAST(dx.deck_copy AS INT) AS deck_copy
        ,  CAST(sx.stash_copy AS INT) AS stash_copy
    FROM deck d
        INNER JOIN (
        SELECT ds.deck_id
            , ds.%1$s_card_id AS card_id
            , ds.index
            , row_number() OVER (PARTITION BY ds.deck_id, ds.%1$s_card_id ORDER BY ds.index) AS deck_copy
        FROM deck_%1$s ds
        ORDER BY ds.deck_id, ds.index
        ) dx ON dx.deck_id = d.id
        LEFT JOIN (
        SELECT sc.deck_id
            , sc.card_id
            , row_number() OVER (PARTITION BY sc.deck_id, sc.card_id ORDER BY sc.id) AS stash_copy
        FROM stashed_card sc
        ORDER BY sc.id
        ) sx ON sx.deck_id = d.id AND sx.card_id = dx.card_id AND dx.deck_copy = sx.stash_copy
    WHERE stash_copy IS NULL
    ORDER BY d.id, dx.index
    $$, $1);
END;
$body$;