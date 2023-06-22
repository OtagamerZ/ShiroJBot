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

CREATE OR REPLACE FUNCTION t_generate_giftcode()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    NEW.code = md5(cast(current_date AS TEXT) || random());
    NEW.gift = '//' || lpad(cast((SELECT count(1) + 1 FROM giftcode) AS TEXT), 4, '0') || E'\n';

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS generate_giftcode ON giftcode;
CREATE TRIGGER generate_giftcode
    BEFORE INSERT
    ON giftcode
    FOR EACH ROW
EXECUTE PROCEDURE t_generate_giftcode();
