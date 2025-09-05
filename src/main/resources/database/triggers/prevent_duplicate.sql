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
    WHERE sc.uuid <> OLD.uuid
      AND sc.kawaipon_uid = OLD.kawaipon_uid
      AND sc.card_id = OLD.card_id
      AND sc.chrome = OLD.chrome
      AND sc.in_collection
    INTO card_id;

    IF (card_id IS NOT NULL) THEN
        RAISE EXCEPTION 'Attempt to insert duplicate card: %', OLD.uuid;
    END IF;

    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS prevent_duplicate ON stashed_card;
CREATE TRIGGER prevent_duplicate
    BEFORE UPDATE
    ON stashed_card
    FOR EACH ROW
    WHEN ( OLD.price <> -1 )
EXECUTE PROCEDURE t_prevent_duplicate();