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

CREATE OR REPLACE FUNCTION t_prevent_bigamy()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF ((SELECT 1 FROM couple c WHERE NEW.first IN (c.first, c.second) OR NEW.second IN (c.first, c.second)) IS NOT NULL) THEN
        RAISE EXCEPTION 'Cannot be in two marriages at the same time';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS prevent_bigamy ON couple;
CREATE TRIGGER prevent_bigamy
    BEFORE INSERT
    ON couple
    FOR EACH ROW
EXECUTE PROCEDURE t_prevent_bigamy();
