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

CREATE OR REPLACE FUNCTION t_remove_deck_references()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    DELETE
    FROM deck_senshi ds
    USING get_missing_deck_references('senshi') x
    WHERE ds.deck_id = x.id
    AND ds.index = x.index;

    DELETE
    FROM deck_evogear de
        USING get_missing_deck_references('evogear') x
    WHERE de.deck_id = x.id
      AND de.index = x.index;

    DELETE
    FROM deck_field df
        USING get_missing_deck_references('field') x
    WHERE df.deck_id = x.id
      AND df.index = x.index;

    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS remove_deck_references ON stashed_card;
CREATE TRIGGER remove_deck_references
    BEFORE DELETE
    ON stashed_card
    FOR EACH ROW
EXECUTE PROCEDURE t_remove_deck_references();
