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

/*CREATE OR REPLACE FUNCTION t_generate_token()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    UPDATE deck d
    SET current = FALSE
    WHERE d.account_uid = NEW.account_uid
    AND d.index <> NEW.index;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS generate_token ON deck;
CREATE TRIGGER generate_token
    BEFORE UPDATE OF current
    ON deck
    FOR EACH ROW
    WHEN ( OLD.current <> NEW.current AND NEW.current IS TRUE )
EXECUTE PROCEDURE t_generate_token();*/
