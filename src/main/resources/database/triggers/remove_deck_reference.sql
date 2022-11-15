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

CREATE OR REPLACE FUNCTION t_remove_deck_reference()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF (NEW IS NULL OR OLD.deck_id <> NEW.deck_id) THEN
        CASE OLD.type
            WHEN 'KAWAIPON' THEN BEGIN
                DELETE
                FROM deck_senshi
                WHERE deck_id = OLD.deck_id
                  AND index IN (
                               SELECT index
                               FROM deck_senshi
                               WHERE deck_id = OLD.deck_id
                                 AND card_id = OLD.card_id
                               LIMIT 1
                               );
            END;
            WHEN 'EVOGEAR' THEN BEGIN
                DELETE
                FROM deck_evogear
                WHERE deck_id = OLD.deck_id
                  AND index IN (
                               SELECT index
                               FROM deck_evogear
                               WHERE deck_id = OLD.deck_id
                                 AND card_id = OLD.card_id
                               LIMIT 1
                               );
            END;
            WHEN 'FIELD' THEN BEGIN
                DELETE
                FROM deck_field
                WHERE deck_id = OLD.deck_id
                  AND index IN (
                               SELECT index
                               FROM deck_field
                               WHERE deck_id = OLD.deck_id
                                 AND card_id = OLD.card_id
                               LIMIT 1
                               );
            END;
            END CASE;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS remove_deck_reference ON deck_senshi;
CREATE TRIGGER remove_deck_reference
    BEFORE UPDATE OR DELETE
    ON stashed_card
    FOR EACH ROW
EXECUTE PROCEDURE t_remove_deck_reference();