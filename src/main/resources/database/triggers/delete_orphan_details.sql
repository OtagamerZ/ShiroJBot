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

CREATE OR REPLACE FUNCTION t_delete_orphan_details()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    DELETE
    FROM card_details
    WHERE card_uuid IN (
                       SELECT cd.card_uuid
                       FROM card_details cd
                                LEFT JOIN kawaipon_card kc ON kc.uuid = cd.card_uuid
                                LEFT JOIN stashed_card sc ON sc.uuid = cd.card_uuid
                       WHERE kc.uuid IS NULL
                         AND sc.uuid IS NULL
                       );

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS delete_orphan_details ON kawaipon.card_details;
CREATE TRIGGER delete_orphan_details
    AFTER INSERT OR UPDATE
    ON kawaipon.card_details
EXECUTE PROCEDURE t_delete_orphan_details();

DROP TRIGGER IF EXISTS delete_orphan_details ON kawaipon.kawaipon_card;
CREATE TRIGGER delete_orphan_details
    AFTER DELETE
    ON kawaipon.kawaipon_card
EXECUTE PROCEDURE t_delete_orphan_details();

DROP TRIGGER IF EXISTS delete_orphan_details ON kawaipon.stashed_card;
CREATE TRIGGER delete_orphan_details
    AFTER DELETE
    ON kawaipon.stashed_card
EXECUTE PROCEDURE t_delete_orphan_details();