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

CREATE OR REPLACE FUNCTION t_fav_stack()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    fav VARCHAR;
BEGIN
    SELECT fav_card FROM kawaipon WHERE uid = NEW.kawaipon_uid INTO fav;

    IF (fav IS NOT NULL AND (SELECT 1 FROM kawaipon_card kc WHERE kc.uuid = NEW.uuid) IS NULL) THEN
        IF (NEW.card_id = fav) THEN
            UPDATE kawaipon
            SET fav_card       = NULL
              , fav_expiration = NULL
              , fav_stacks     = 0
            WHERE uid = NEW.kawaipon_uid;
        ELSE
            UPDATE kawaipon
            SET fav_stacks = fav_stacks + 1
            WHERE uid = NEW.kawaipon_uid;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS fav_stack ON kawaipon_card;
CREATE TRIGGER fav_stack
    BEFORE INSERT
    ON kawaipon_card
    FOR EACH ROW
EXECUTE PROCEDURE t_fav_stack();

DROP TRIGGER IF EXISTS fav_stack ON stashed_card;
CREATE TRIGGER fav_stack
    BEFORE INSERT
    ON stashed_card
    FOR EACH ROW
EXECUTE PROCEDURE t_fav_stack();
