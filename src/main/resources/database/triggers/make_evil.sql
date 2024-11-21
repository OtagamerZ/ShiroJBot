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

CREATE OR REPLACE FUNCTION t_make_evil()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    ids VARCHAR[];
BEGIN
    SELECT array_agg(g.id)
    FROM gear g
             INNER JOIN hero h ON h.id = g.owner_id
    WHERE h.id = ?1
      AND NOT jsonb_path_exists(h.equipment, '$.* ? (@ == $val)', cast('{"val": ' || g.id || '}' AS JSONB))
    INTO ids;

    DELETE
    FROM gear_affix
    WHERE gear_id = ANY(ids);

    DELETE
    FROM gear
    WHERE id = ANY(ids);

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS make_evil ON hero;
CREATE TRIGGER make_evil
    AFTER UPDATE OF evil
    ON hero
    FOR EACH ROW
    WHEN ( NOT OLD.evil AND NEW.evil )
EXECUTE PROCEDURE t_make_evil();