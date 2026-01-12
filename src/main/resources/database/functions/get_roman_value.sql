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

CREATE OR REPLACE FUNCTION dunhun.get_roman_value_char(CHAR(1))
    RETURNS INT
    LANGUAGE sql
AS
$$
SELECT CASE upper($1)
           WHEN 'I' THEN 1
           WHEN 'V' THEN 5
           WHEN 'X' THEN 10
           WHEN 'L' THEN 50
           WHEN 'C' THEN 100
           WHEN 'D' THEN 500
           WHEN 'M' THEN 1000
           END;
$$;

CREATE OR REPLACE FUNCTION dunhun.get_roman_value(VARCHAR)
    RETURNS INT
    LANGUAGE plpgsql
AS
$$
DECLARE
    value INT;
    len   INT;
    i     INT;
    c     CHAR(1);
BEGIN
    len := length($1);
    value := 0;

    FOR i IN 1..len
        LOOP
            c := substring($1 from i for 1);
            IF (i + 1 <= len AND get_roman_value_char(c) < get_roman_value_char(substring($1 from i + 1 for 1))) THEN
                value := value - dunhun.get_roman_value_char(c);
            ELSE
                value := value + dunhun.get_roman_value_char(c);
            END IF;
        END LOOP;

    RETURN value;
END;
$$;