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

CREATE OR REPLACE FUNCTION t_generate_kawaipon()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF ((SELECT 1 FROM kawaipon_card kc WHERE kc.uuid = NEW.uuid) IS NULL) THEN
        INSERT INTO kawaipon_card (chrome, quality, uuid, card_id, kawaipon_uid)
        VALUES (false, 0, NEW.uuid, NEW.card_id, NEW.kawaipon_uid);
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS generate_kawaipon ON stashed_card;
CREATE TRIGGER generate_kawaipon
    AFTER INSERT
    ON stashed_card
    FOR EACH ROW
    WHEN ( NEW.type = 'KAWAIPON' )
EXECUTE PROCEDURE t_generate_kawaipon();
