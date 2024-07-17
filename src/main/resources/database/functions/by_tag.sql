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

CREATE OR REPLACE FUNCTION by_tag(VARCHAR, VARCHAR[])
    RETURNS SETOF VARCHAR
    LANGUAGE plpgsql
AS
$body$
BEGIN
    RETURN QUERY EXECUTE format($$
        SELECT card_id
        FROM %1$s
        WHERE tags \?& '%2$s'
        %3$s
        $$, $1, $2, iif(
            lower($1) = 'senshi',
            cast('OR race = ANY(''{' || array_to_string($2, ',') || '}'')' AS VARCHAR),
            cast('' AS VARCHAR)
        ));
END
$body$;