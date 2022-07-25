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

CREATE OR REPLACE PROCEDURE fix_deck_gaps(VARCHAR)
    LANGUAGE plpgsql
AS
$$
BEGIN
    UPDATE deck_senshi
    SET index = x.expected
    FROM (
         SELECT ds.deck_id
              , ds.senshi_card_id
              , ds.index AS current
              , row_number() OVER (PARTITION BY ds.deck_id) - 1 AS expected
         FROM deck_senshi ds
                  INNER JOIN deck d ON d.id = ds.deck_id
         WHERE d.account_uid = $1
         ORDER BY ds.deck_id, ds.index
    ) x
    WHERE x.current != x.expected;

    UPDATE deck_evogear
    SET index = x.expected
    FROM (
         SELECT de.deck_id
              , de.evogear_card_id
              , de.index AS current
              , row_number() OVER (PARTITION BY de.deck_id) - 1 AS expected
         FROM deck_evogear de
                  INNER JOIN deck d ON d.id = de.deck_id
         WHERE d.account_uid = $1
         ORDER BY de.deck_id, de.index
         ) x
    WHERE x.current != x.expected;

    UPDATE deck_field
    SET index = x.expected
    FROM (
         SELECT df.deck_id
              , df.field_card_id
              , df.index AS current
              , row_number() OVER (PARTITION BY df.deck_id) - 1 AS expected
         FROM deck_field df
                  INNER JOIN deck d ON d.id = df.deck_id
         WHERE d.account_uid = $1
         ORDER BY df.deck_id, df.index
         ) x
    WHERE x.current != x.expected;
END
$$;