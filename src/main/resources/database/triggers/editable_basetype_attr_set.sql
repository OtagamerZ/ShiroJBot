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

CREATE OR REPLACE FUNCTION t_update_basetype_attr_set()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    level INT;
    total INT;
BEGIN
    level = 1 + 21 * (NEW.tier - 1);
    total = 5 * NEW.tier;

    UPDATE basetype
    SET req_level      = level
      , req_attributes = (cast(total * NEW.str AS INT) & cast(x'FF' AS INT))
        + ((cast(total * NEW.dex AS INT) & cast(x'FF' AS INT)) << 8)
        + ((cast(total * NEW.wis AS INT) & cast(x'FF' AS INT)) << 16)
        + ((cast(total * NEW.vit AS INT) & cast(x'FF' AS INT)) << 24)
    WHERE id = NEW.id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS update_basetype_attr_set ON v_basetype_attr_set;
CREATE TRIGGER update_basetype_attr_set
    INSTEAD OF UPDATE
    ON v_basetype_attr_set
    FOR EACH ROW
EXECUTE PROCEDURE t_update_basetype_attr_set();