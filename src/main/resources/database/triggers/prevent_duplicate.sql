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

CREATE OR REPLACE FUNCTION t_prevent_duplicate()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    card_id VARCHAR;
BEGIN
    SELECT sc.card_id
    FROM stashed_card sc
    WHERE sc.uuid <> NEW.uuid
      AND sc.kawaipon_uid = NEW.kawaipon_uid
      AND sc.card_id = NEW.card_id
      AND sc.chrome = NEW.chrome
      AND sc.in_collection
    INTO card_id;

    IF (card_id IS NOT NULL) THEN
        RAISE EXCEPTION 'Attempt to insert duplicate card: %', NEW.uuid;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS prevent_duplicate ON stashed_card;
CREATE TRIGGER prevent_duplicate
    BEFORE UPDATE
    ON stashed_card
    FOR EACH ROW
    WHEN ( OLD.price <> -1 )
EXECUTE PROCEDURE t_prevent_duplicate();