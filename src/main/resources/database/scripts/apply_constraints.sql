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

CREATE OR REPLACE PROCEDURE apply_constraints()
    LANGUAGE plpgsql
AS
$body$
DECLARE
    _match RECORD;
BEGIN
    FOR _match IN
        SELECT x.name
             , x.table
             , x.def
             , m[1] AS col
             , m[2] AS ref
        FROM (
             SELECT r.conname                              AS name
                  , cast(r.conrelid AS regclass)           AS "table"
                  , pg_catalog.pg_get_constraintdef(r.oid) AS def
             FROM pg_catalog.pg_constraint r
             WHERE r.contype = 'f'
             ) x
           , regexp_match(x.def, 'FOREIGN KEY \((\w+)\) REFERENCES (\w+)') m
        WHERE def SIMILAR TO '% card\(id\)|%\(card_id\)'
        LOOP
            EXECUTE format($$
            ALTER TABLE "%1$I"
                DROP CONSTRAINT "%2$I";

            ALTER TABLE "%1$I"
                ADD CONSTRAINT "%2$I"
                    FOREIGN KEY ("%3$I") REFERENCES "%4$I"
                        ON UPDATE CASCADE;
            $$, _match.table, _match.name, _match.col, _match.ref);
        END LOOP;
END
$body$;

CALL apply_constraints();
