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

CREATE OR REPLACE FUNCTION t_switch_active_title()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    UPDATE account_title d
    SET current = FALSE
    WHERE d.account_uid = NEW.account_uid
    AND d.id <> NEW.id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS switch_active_title ON account_title;
CREATE TRIGGER switch_active_title
    BEFORE UPDATE OF current
    ON account_title
    FOR EACH ROW
    WHEN ( OLD.current <> NEW.current AND NEW.current IS TRUE )
EXECUTE PROCEDURE t_switch_active_title();
