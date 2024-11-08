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

CREATE OR REPLACE FUNCTION t_insert_basetype_i18n()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF (NEW.name_pt IS NOT NULL) THEN
        INSERT INTO basetype_info (id, locale, name) VALUES (NEW.id, 'PT', NEW.name_pt);
    END IF;

    IF (NEW.name_en IS NOT NULL) THEN
        INSERT INTO basetype_info (id, locale, name) VALUES (NEW.id, 'EN', NEW.name_en);
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS insert_basetype_i18n ON v_basetype_i18n;
CREATE TRIGGER insert_basetype_i18n
    INSTEAD OF INSERT
    ON v_basetype_i18n
    FOR EACH ROW
EXECUTE PROCEDURE t_insert_basetype_i18n();

CREATE OR REPLACE FUNCTION t_update_basetype_i18n()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF (NEW.name_pt IS NOT NULL) THEN
        INSERT INTO basetype_info (id, locale, name)
        VALUES (NEW.id, 'PT', NEW.name_pt)
        ON CONFLICT (id, locale) DO UPDATE SET name = excluded.name;
    ELSE
        DELETE FROM basetype_info WHERE id = NEW.id AND locale = 'PT';
    END IF;

    IF (NEW.name_en IS NOT NULL) THEN
        INSERT INTO basetype_info (id, locale, name)
        VALUES (NEW.id, 'EN', NEW.name_en)
        ON CONFLICT (id, locale) DO UPDATE SET name = excluded.name;
    ELSE
        DELETE FROM basetype_info WHERE id = NEW.id AND locale = 'EN';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS update_basetype_i18n ON v_basetype_i18n;
CREATE TRIGGER update_basetype_i18n
    INSTEAD OF UPDATE
    ON v_basetype_i18n
    FOR EACH ROW
EXECUTE PROCEDURE t_update_basetype_i18n();

CREATE OR REPLACE FUNCTION t_delete_basetype_i18n()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    DELETE FROM basetype_info WHERE id = OLD.id;
    RETURN NEW;
END
$$;

DROP TRIGGER IF EXISTS delete_basetype_i18n ON v_basetype_i18n;
CREATE TRIGGER delete_basetype_i18n
    INSTEAD OF DELETE
    ON v_basetype_i18n
    FOR EACH ROW
EXECUTE PROCEDURE t_delete_basetype_i18n();