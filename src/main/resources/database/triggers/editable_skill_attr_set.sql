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
BEGIN
    UPDATE skill
    SET req_race = NEW.req_race
      , req_weapons = NEW.req_weapons
      , attributes = (NEW.str & cast(x'FF' AS INT))
                         | ((NEW.dex & cast(x'FF' AS INT)) << 8)
                         | ((NEW.wis & cast(x'FF' AS INT)) << 16)
                         | ((NEW.vit & cast(x'FF' AS INT)) << 24)
    WHERE id = NEW.id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS update_skill_attr_set ON v_desc_i18n;
CREATE TRIGGER update_skill_attr_set
    INSTEAD OF UPDATE
    ON v_skill_attr_set
    FOR EACH ROW
EXECUTE PROCEDURE t_update_skill_attr_set();