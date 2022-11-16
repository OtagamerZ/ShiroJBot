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

CREATE OR REPLACE PROCEDURE clear_ghosts(INT)
    LANGUAGE plpgsql
AS
$$
BEGIN
    DELETE FROM deck_senshi
    WHERE deck_id = $1
    AND index IN (
                 SELECT a.index
                 FROM deck d
                          LEFT JOIN (
                                    SELECT deck_id
                                         , senshi_card_id                                  AS card_id
                                         , row_number() OVER (PARTITION BY senshi_card_id) AS copy
                                         , index
                                    FROM deck_senshi
                                    WHERE deck_id = $1
                                    ORDER BY card_id, copy
                                    ) a ON d.id = a.deck_id
                          LEFT JOIN (
                                    SELECT deck_id
                                         , card_id
                                         , row_number() OVER (PARTITION BY card_id) AS copy
                                    FROM stashed_card
                                    WHERE deck_id = $1
                                    ORDER BY card_id, copy
                                    ) b ON d.id = b.deck_id AND a.card_id = b.card_id AND a.copy = b.copy
                 WHERE d.id = $1
                   AND b.copy IS NULL
        );

    DELETE FROM deck_evogear
    WHERE deck_id = $1
      AND index IN (
                   SELECT a.index
                   FROM deck d
                            LEFT JOIN (
                                      SELECT deck_id
                                           , evogear_card_id                                  AS card_id
                                           , row_number() OVER (PARTITION BY evogear_card_id) AS copy
                                           , index
                                      FROM deck_evogear
                                      WHERE deck_id = $1
                                      ORDER BY card_id, copy
                                      ) a ON d.id = a.deck_id
                            LEFT JOIN (
                                      SELECT deck_id
                                           , card_id
                                           , row_number() OVER (PARTITION BY card_id) AS copy
                                      FROM stashed_card
                                      WHERE deck_id = $1
                                      ORDER BY card_id, copy
                                      ) b ON d.id = b.deck_id AND a.card_id = b.card_id AND a.copy = b.copy
                   WHERE d.id = $1
                     AND b.copy IS NULL
                   );

    DELETE FROM deck_field
    WHERE deck_id = $1
      AND index IN (
                   SELECT a.index
                   FROM deck d
                            LEFT JOIN (
                                      SELECT deck_id
                                           , field_card_id                                  AS card_id
                                           , row_number() OVER (PARTITION BY field_card_id) AS copy
                                           , index
                                      FROM deck_field
                                      WHERE deck_id = $1
                                      ORDER BY card_id, copy
                                      ) a ON d.id = a.deck_id
                            LEFT JOIN (
                                      SELECT deck_id
                                           , card_id
                                           , row_number() OVER (PARTITION BY card_id) AS copy
                                      FROM stashed_card
                                      WHERE deck_id = $1
                                      ORDER BY card_id, copy
                                      ) b ON d.id = b.deck_id AND a.card_id = b.card_id AND a.copy = b.copy
                   WHERE d.id = $1
                     AND b.copy IS NULL
                   );
END
$$;