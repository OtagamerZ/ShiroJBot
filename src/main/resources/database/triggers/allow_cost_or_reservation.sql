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

CREATE OR REPLACE FUNCTION t_allow_cost_or_reservation()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    RAISE EXCEPTION 'Skills can only either have a cost or a reservation, not both';
END;
$$;

DROP TRIGGER IF EXISTS allow_cost_or_reservation ON skill;
CREATE TRIGGER allow_cost_or_reservation
    BEFORE UPDATE
    ON skill
    FOR EACH ROW
    WHEN ( NEW.cost > 0 AND NEW.reservation > 0 )
EXECUTE PROCEDURE t_allow_cost_or_reservation();
