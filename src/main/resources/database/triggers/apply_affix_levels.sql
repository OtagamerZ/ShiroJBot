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

CREATE OR REPLACE FUNCTION t_apply_affix_levels()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF ((
        SELECT min(id) FROM affix WHERE get_affix_family(id) = get_affix_family(NEW.id)
        ) <> NEW.id) THEN
        RETURN NEW;
    END IF;

    UPDATE affix a
    SET min_level = x.new_level
    FROM (
         SELECT x.id
              , cast(greatest(1, least(min + (84.0 - min) / 5 * x.idx, 84)) AS INT) AS new_level
         FROM (
              SELECT id
                   , min_level
                   , min(min_level) OVER fam   AS min
                   , row_number() OVER fam - 1 AS idx
              FROM affix
              WHERE weight > 0
                AND get_affix_family(id) = get_affix_family(NEW.id)
              WINDOW fam AS (PARTITION BY get_affix_family(id) ORDER BY id)
              ) x
         OFFSET 1
         ) x
    WHERE a.id = x.id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS apply_affix_levels ON affix;
CREATE TRIGGER apply_affix_levels
    AFTER UPDATE OF min_level
    ON affix
    FOR EACH ROW
EXECUTE PROCEDURE t_apply_affix_levels();
