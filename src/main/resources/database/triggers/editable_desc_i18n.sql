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

CREATE OR REPLACE FUNCTION t_insert_desc_i18n()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF (NEW.desc_pt IS NOT NULL) THEN
        INSERT INTO kawaipon.card_descriptions (id, locale, description) VALUES (NEW.id, 'PT', NEW.desc_pt);
    END IF;

    IF (NEW.desc_en IS NOT NULL) THEN
        INSERT INTO kawaipon.card_descriptions (id, locale, description) VALUES (NEW.id, 'EN', NEW.desc_en);
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS insert_desc_i18n ON v_desc_i18n;
CREATE TRIGGER insert_desc_i18n
    INSTEAD OF INSERT
    ON v_desc_i18n
    FOR EACH ROW
EXECUTE PROCEDURE t_insert_desc_i18n();

CREATE OR REPLACE FUNCTION t_update_desc_i18n()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF (NEW.desc_pt IS NOT NULL) THEN
        INSERT INTO kawaipon.card_descriptions (id, locale, description)
        VALUES (NEW.id, 'PT', NEW.desc_pt)
        ON CONFLICT (id, locale) DO UPDATE SET description = excluded.description;
    ELSE
        DELETE FROM card_descriptions WHERE id = NEW.id AND locale = 'PT';
    END IF;

    IF (NEW.desc_en IS NOT NULL) THEN
        INSERT INTO kawaipon.card_descriptions (id, locale, description)
        VALUES (NEW.id, 'EN', NEW.desc_en)
        ON CONFLICT (id, locale) DO UPDATE SET description = excluded.description;
    ELSE
        DELETE FROM kawaipon.card_descriptions WHERE id = NEW.id AND locale = 'EN';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS update_desc_i18n ON v_desc_i18n;
CREATE TRIGGER update_desc_i18n
    INSTEAD OF UPDATE
    ON v_desc_i18n
    FOR EACH ROW
EXECUTE PROCEDURE t_update_desc_i18n();

CREATE OR REPLACE FUNCTION t_delete_desc_i18n()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    DELETE FROM kawaipon.card_descriptions WHERE id = OLD.id;
    RETURN NEW;
END
$$;

DROP TRIGGER IF EXISTS delete_desc_i18n ON v_desc_i18n;
CREATE TRIGGER delete_desc_i18n
    INSTEAD OF DELETE
    ON v_desc_i18n
    FOR EACH ROW
EXECUTE PROCEDURE t_delete_desc_i18n();