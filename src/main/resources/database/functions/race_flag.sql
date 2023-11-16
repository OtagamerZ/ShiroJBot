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

CREATE OR REPLACE FUNCTION race_flag(VARCHAR)
    RETURNS BIT
    LANGUAGE sql
AS
$$
SELECT CASE $1
           WHEN 'HUMAN'     THEN b'00000001'
           WHEN 'BEAST'     THEN b'00000010'
           WHEN 'MACHINE'   THEN b'00000100'
           WHEN 'DIVINITY'  THEN b'00001000'
           WHEN 'SPIRIT'    THEN b'00010000'
           WHEN 'UNDEAD'    THEN b'00100000'
           WHEN 'MYSTICAL'  THEN b'01000000'
           WHEN 'DEMON'     THEN b'10000000'
           ELSE b'00000000'
           END
$$;
