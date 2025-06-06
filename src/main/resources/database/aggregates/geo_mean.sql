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

CREATE OR REPLACE FUNCTION geo_mean_accum(NUMERIC[], NUMERIC, NUMERIC DEFAULT -1)
    RETURNS NUMERIC[]
    LANGUAGE sql
AS
$$
SELECT ARRAY [$1[1] + ln($2), iif($3 < 0, $1[2] + 1.0, $3)];
$$;

CREATE OR REPLACE FUNCTION geo_mean_accum(NUMERIC[], NUMERIC) RETURNS NUMERIC[]
    LANGUAGE sql
AS
$$
SELECT ARRAY [$1[1] + ln($2), $1[2] + 1.0];
$$;

CREATE OR REPLACE FUNCTION geo_mean_finalize(NUMERIC[])
    RETURNS NUMERIC
    LANGUAGE sql
AS
$$
SELECT exp($1[1] / $1[2]);
$$;

-- CREATE AGGREGATE geo_mean(NUMERIC, NUMERIC) (
--     SFUNC = geo_mean_accum,
--     STYPE = NUMERIC[],
--     FINALFUNC = geo_mean_finalize,
--     INITCOND = '{0.0, 0.0}'
--     );

-- CREATE AGGREGATE geo_mean(NUMERIC) (
--     SFUNC = geo_mean_accum,
--     STYPE = NUMERIC[],
--     FINALFUNC = geo_mean_finalize,
--     INITCOND = '{0.0, 0.0}'
--     );