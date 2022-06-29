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

CREATE OR REPLACE FUNCTION t_generate_token()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF (trim(NEW.bearer) = '') THEN
        RAISE 'a bearer must be supplied';
    ELSEIF (TG_OP = 'INSERT' AND (NEW.token <> '' OR NEW.salt <> '')) THEN
        RETURN OLD;
    END IF;

    IF (NEW.salt <> OLD.salt) THEN
        NEW.salt = gen_salt('sha1');
    END IF;

    NEW.token = encode(cast(NEW.bearer AS bytea), 'base64')
        ||'.'||
        encode(cast(cast(extract(MILLISECONDS FROM now()) AS text) AS bytea), 'base64')
        ||'.'||
        encode(hmac(NEW.bearer, NEW.salt, 'sha1'), 'base64');

    RETURN NEW;
END
$$;

DROP TRIGGER IF EXISTS generate_token ON access_token;
CREATE TRIGGER generate_token
    BEFORE INSERT
    ON access_token
EXECUTE PROCEDURE t_generate_token();

DROP TRIGGER IF EXISTS regenerate_token ON access_token;
CREATE TRIGGER regenerate_token
    BEFORE UPDATE
    ON access_token
EXECUTE PROCEDURE t_generate_token();