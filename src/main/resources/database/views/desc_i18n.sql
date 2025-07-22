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

-- DROP VIEW IF EXISTS kawaipon.v_desc_i18n;
CREATE OR REPLACE VIEW kawaipon.v_desc_i18n AS
SELECT coalesce(pt.id, en.id) AS id
     , pt.description AS desc_pt
     , en.description AS desc_en
FROM card_descriptions pt
         FULL JOIN card_descriptions en ON en.id = pt.id AND en.locale = 'EN'
WHERE pt.locale = 'PT'
ORDER BY id
