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

CREATE OR REPLACE FUNCTION dunhun.get_attribute_class(INT)
    RETURNS VARCHAR
    LANGUAGE sql
AS
$$
SELECT string_agg(x.key, '-')
FROM (
     SELECT split_part(x.attr, '=', 1)              AS key
          , cast(split_part(x.attr, '=', 2) AS INT) AS value
     FROM (
          SELECT unnest(string_to_array(
                  'STR=' || bit_get($1, 8, 0) || ';'
                      || 'DEX=' || bit_get($1, 8, 1) || ';'
                      || 'WIS=' || bit_get($1, 8, 2) || ';'
                      || 'VIT=' || bit_get($1, 8, 3)
              , ';')) AS attr
          ) x
     ) x
WHERE x.value > 0
$$;