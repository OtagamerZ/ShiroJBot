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

CREATE OR REPLACE FUNCTION t_update_card_count()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    ref card;
BEGIN
    ref = COALESCE(OLD, NEW);

    IF NOT EXISTS(SELECT FROM aux.card_counter WHERE id = ref.anime_id) THEN
        INSERT
        INTO aux.card_counter (id, count)
        VALUES (ref.anime_id, (SELECT COUNT(1) FROM card WHERE anime_id = ref.anime_id AND get_rarity_index(rarity) < 6));
    END IF;

    IF TG_OP = 'INSERT' THEN
        UPDATE aux.card_counter SET count = count + 1 WHERE id = NEW.anime_id;
    ELSIF TG_OP = 'UPDATE' AND OLD.anime_id <> NEW.anime_id THEN
        UPDATE aux.card_counter SET count = count - 1 WHERE id = OLD.anime_id;
        UPDATE aux.card_counter SET count = count + 1 WHERE id = NEW.anime_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE aux.card_counter SET count = count - 1 WHERE id = OLD.anime_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS update_card_count ON card;
CREATE TRIGGER update_card_count
    BEFORE INSERT OR UPDATE OR DELETE
    ON card
    FOR EACH ROW
EXECUTE PROCEDURE t_update_card_count();
