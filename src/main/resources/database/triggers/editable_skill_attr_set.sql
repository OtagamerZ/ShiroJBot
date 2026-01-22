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

CREATE OR REPLACE FUNCTION t_update_skill_attr_set()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    total INT;
    parts INT;
    each  INT;
BEGIN
    total = (4 + NEW.tier) * NEW.tier;
    parts = bool_value(NEW.str) + bool_value(NEW.dex) + bool_value(NEW.wis) + bool_value(NEW.vit);
    each = iif(parts > 0, total / parts, 0);

    UPDATE skill
    SET req_tags       = NEW.req_tags
      , req_attributes = (iif(NEW.str, each, 0) & cast(x'FF' AS INT))
        + ((iif(NEW.dex, each, 0) & cast(x'FF' AS INT)) << 8)
        + ((iif(NEW.wis, each, 0) & cast(x'FF' AS INT)) << 16)
        + ((iif(NEW.vit, each, 0) & cast(x'FF' AS INT)) << 24)
    WHERE id = NEW.id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS update_skill_attr_set ON v_skill_attr_set;
CREATE TRIGGER update_skill_attr_set
    INSTEAD OF UPDATE
    ON v_skill_attr_set
    FOR EACH ROW
EXECUTE PROCEDURE t_update_skill_attr_set();