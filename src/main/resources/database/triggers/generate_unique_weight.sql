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

CREATE OR REPLACE FUNCTION t_generate_unique_weight()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    SELECT avg(a.weight * 1.5) - avg(a.weight * 1.5) % 25
    FROM "unique" u
             INNER JOIN affix a ON has(u.affixes, a.id)
    WHERE u.id = NEW.id
      AND a.weight > 0
    INTO NEW.weight;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS generate_unique_weight ON "unique";
CREATE TRIGGER generate_unique_weight
    BEFORE UPDATE OF weight OR INSERT
    ON "unique"
    FOR EACH ROW
    WHEN (NEW.weight = -1)
EXECUTE PROCEDURE t_generate_unique_weight();
