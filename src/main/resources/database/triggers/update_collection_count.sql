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

CREATE OR REPLACE FUNCTION t_update_collection_count()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    _mod INT;
BEGIN
    INSERT INTO aux.collection_counter (uid, anime_id, normal, chrome)
    SELECT kawaipon_uid
         , anime_id
         , normal
         , chrome
    FROM aux.v_collection_counter
    WHERE anime_id IN (SELECT anime_id FROM card WHERE id IN (OLD.card_id, NEW.card_id))
    ON CONFLICT DO NOTHING;

    IF TG_OP = 'INSERT' THEN
        UPDATE aux.v_collection_counter
        SET normal = normal + CAST(NOT NEW.chrome AS INT)
          , chrome = chrome + CAST(NEW.chrome AS INT)
        WHERE kawaipon_uid = NEW.kawaipon_uid
          AND anime_id = (SELECT anime_id FROM card WHERE id = NEW.card_id);
    ELSIF TG_OP = 'UPDATE' THEN
        IF (OLD.card_id <> NEW.card_id) THEN
            RAISE EXCEPTION 'Cannot change card ID';
        END IF;

        IF (OLD.kawaipon_uid <> NEW.kawaipon_uid) THEN
            UPDATE aux.v_collection_counter
            SET normal = normal - iif(NEW.chrome, 0, 1)
              , chrome = chrome - iif(NEW.chrome, 1, 0)
            WHERE kawaipon_uid = OLD.kawaipon_uid
              AND anime_id = (SELECT anime_id FROM card WHERE id = OLD.card_id);

            UPDATE aux.v_collection_counter
            SET normal = normal + iif(NEW.chrome, 0, 1)
              , chrome = chrome + iif(NEW.chrome, 1, 0)
            WHERE kawaipon_uid = NEW.kawaipon_uid
              AND anime_id = (SELECT anime_id FROM card WHERE id = NEW.card_id);
        END IF;

        IF (OLD.chrome <> NEW.chrome) THEN
            _mod = iif(NEW.chrome, 1, -1);

            UPDATE aux.v_collection_counter
            SET normal = normal - 1 * _mod
              , chrome = chrome + 1 * _mod
            WHERE kawaipon_uid = NEW.kawaipon_uid
              AND anime_id = (SELECT anime_id FROM card WHERE id = NEW.card_id);
        END IF;

        IF (OLD.stash_entry <> NEW.stash_entry) THEN
            _mod = iif(NEW.stash_entry IS NULL, 1, -1);

            UPDATE aux.v_collection_counter
            SET normal = normal + iif(NEW.chrome, 0, 1) * _mod
              , chrome = chrome + iif(NEW.chrome, 1, 0) * _mod
            WHERE kawaipon_uid = NEW.kawaipon_uid
              AND anime_id = (SELECT anime_id FROM card WHERE id = NEW.card_id);
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE aux.v_collection_counter
        SET normal = normal - CAST(NOT OLD.chrome AS INT)
          , chrome = chrome - CAST(OLD.chrome AS INT)
        WHERE kawaipon_uid = OLD.kawaipon_uid
          AND anime_id = (SELECT anime_id FROM card WHERE id = OLD.card_id);
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS update_collection_count ON kawaipon_card;
CREATE TRIGGER update_collection_count
    BEFORE INSERT OR UPDATE OR DELETE
    ON kawaipon_card
    FOR EACH ROW
EXECUTE PROCEDURE t_update_collection_count();