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

CREATE OR REPLACE FUNCTION t_sync_stash_entry()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    UPDATE kawaipon_card kc
    SET kawaipon_uid = NEW.kawaipon_uid
    WHERE kc.stash_entry = NEW.id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS sync_stash_entry ON stashed_card;
CREATE TRIGGER sync_stash_entry
    BEFORE UPDATE OF kawaipon_uid
    ON stashed_card
    FOR EACH ROW
    WHEN ( OLD.kawaipon_uid <> NEW.kawaipon_uid )
EXECUTE PROCEDURE t_sync_stash_entry();
