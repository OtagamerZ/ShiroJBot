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

CREATE OR REPLACE FUNCTION t_apply_debt()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF (NEW.debit < 0) THEN
        NEW.debit = -NEW.debit;
    END IF;
    IF (NEW.balance < 0) THEN
        NEW.debit = NEW.debit + -NEW.balance;
        NEW.balance = 0;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS apply_debt ON account;
CREATE TRIGGER apply_debt
    BEFORE UPDATE
    ON account
    FOR EACH ROW
EXECUTE PROCEDURE t_apply_debt();
