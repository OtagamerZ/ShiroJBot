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

CREATE OR REPLACE FUNCTION t_generate_token()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    NEW.bearer = trim(NEW.bearer);
    IF (NEW.bearer = '') THEN
        RAISE EXCEPTION 'a bearer must be supplied';
    ELSEIF (NEW.bearer <> OLD.bearer) THEN
        NEW.token = '';
        NEW.salt = '';
    END IF;

    IF (TG_OP = 'INSERT' OR NEW.token <> OLD.token OR NEW.salt <> OLD.salt) THEN
        IF (TG_OP = 'INSERT' OR NEW.salt <> OLD.salt) THEN
            NEW.salt = encode(cast(gen_salt('des') AS bytea), 'hex');
        END IF;

        NEW.token = encode(cast(NEW.bearer AS bytea), 'base64')
            ||'.'||
            encode(cast(cast(extract(MILLISECONDS FROM now()) AS text) AS bytea), 'base64')
            ||'.'||
            encode(hmac(NEW.bearer, NEW.salt, 'sha1'), 'base64');
    END IF;

    RETURN NEW;
END
$$;

DROP TRIGGER IF EXISTS generate_token ON access_token;
CREATE TRIGGER generate_token
    BEFORE INSERT OR UPDATE
    ON access_token
    FOR EACH ROW
EXECUTE PROCEDURE t_generate_token();