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
             , x."table"
             , x.def
             , x.col
             , (SELECT table_schema FROM information_schema.tables WHERE table_name = x.table AND table_schema NOT LIKE '%_old') AS src_sch
             , x.tgt_sch
             , x.ref
             , x.tgt
        FROM (
             SELECT x.name
                  , replace(x.table, '"', '')                    AS "table"
                  , x.def
                  , replace(m[1], '"', '')                       AS col
                  , replace(coalesce(m[2], x."schema"), '"', '') AS tgt_sch
                  , replace(m[3], '"', '')                       AS ref
                  , replace(m[4], '"', '')                       AS tgt
             FROM (
                  SELECT r.conname                                             AS name
                       , pg_get_constraintdef(r.oid)                           AS def
                       , cast(cast(r.conrelid AS regclass) AS VARCHAR)         AS "table"
                       , cast(cast(c.relnamespace AS regnamespace) AS VARCHAR) AS "schema"
                  FROM pg_constraint r
                           INNER JOIN pg_class c ON c.oid = r.confrelid
                  WHERE r.contype = 'f'
                  ) x
                , regexp_match(x.def, 'FOREIGN KEY \(([\w"]+)\) REFERENCES ([\w"]+(?=\.))?([\w"]+)\((\w+?)\)') m
             WHERE x.schema NOT LIKE '%\_old'
             ) x
        WHERE x.ref = 'card'
           OR x.tgt_sch = 'dunhun'
        LOOP
            EXECUTE format($$
                        ALTER TABLE %4$I.%1$I
                            DROP CONSTRAINT %2$I;

                        ALTER TABLE %4$I.%1$I
                            ADD CONSTRAINT %2$I
                                FOREIGN KEY (%3$I) REFERENCES %5$I.%6$I(%7$I)
                                    ON UPDATE CASCADE;
                        $$, _match.table, _match.name, _match.col, _match.src_sch, _match.tgt_sch, _match.ref,
                           _match.tgt
                    );
        END LOOP;
END
$body$;

CALL apply_constraints();