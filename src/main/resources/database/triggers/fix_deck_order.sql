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

CREATE OR REPLACE FUNCTION t_fix_deck_order()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    UPDATE deck_senshi ds
    SET index = x.proper
    FROM (
         SELECT deck_id
              , senshi_card_id
              , index
              , row_number() OVER (PARTITION BY deck_id ORDER BY index) - 1 AS proper
         FROM deck_senshi
         ) x
    WHERE ds.deck_id = x.deck_id
      AND ds.senshi_card_id = x.senshi_card_id
      AND ds.index = x.index
      AND ds.index <> x.proper;

    UPDATE deck_evogear ds
    SET index = x.proper
    FROM (
         SELECT deck_id
              , evogear_card_id
              , index
              , row_number() OVER (PARTITION BY deck_id ORDER BY index) - 1 AS proper
         FROM deck_evogear
         ) x
    WHERE ds.deck_id = x.deck_id
      AND ds.evogear_card_id = x.evogear_card_id
      AND ds.index = x.index
      AND ds.index <> x.proper;

    UPDATE deck_field ds
    SET index = x.proper
    FROM (
         SELECT deck_id
              , field_card_id
              , index
              , row_number() OVER (PARTITION BY deck_id ORDER BY index) - 1 AS proper
         FROM deck_field
         ) x
    WHERE ds.deck_id = x.deck_id
      AND ds.field_card_id = x.field_card_id
      AND ds.index = x.index
      AND ds.index <> x.proper;

    RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS fix_deck_order ON deck_senshi;
CREATE TRIGGER fix_deck_order
    AFTER UPDATE OR INSERT OR DELETE
    ON deck_senshi
    FOR EACH ROW
EXECUTE PROCEDURE t_fix_deck_order();

DROP TRIGGER IF EXISTS fix_deck_order ON deck_evogear;
CREATE TRIGGER fix_deck_order
    AFTER UPDATE OR INSERT OR DELETE
    ON deck_evogear
    FOR EACH ROW
EXECUTE PROCEDURE t_fix_deck_order();

DROP TRIGGER IF EXISTS fix_deck_order ON deck_field;
CREATE TRIGGER fix_deck_order
    AFTER UPDATE OR INSERT OR DELETE
    ON deck_field
    FOR EACH ROW
EXECUTE PROCEDURE t_fix_deck_order();