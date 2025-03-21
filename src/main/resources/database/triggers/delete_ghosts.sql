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

CREATE OR REPLACE FUNCTION t_delete_ghosts()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    DELETE FROM stashed_card sc
           WHERE sc.type = 'KAWAIPON'
             AND NOT exists(SELECT 1 FROM kawaipon_card kc WHERE kc.uuid = sc.uuid);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS delete_ghosts ON kawaipon_card;
CREATE TRIGGER delete_ghosts
    AFTER DELETE
    ON kawaipon_card
EXECUTE PROCEDURE t_delete_ghosts();
