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

package com.kuuhaku.model.common.drop;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.util.Calc;

public class CreditDrop extends Drop<Integer> {
	public CreditDrop(I18N locale) {
		this(locale, Calc.rng(350, 500));
	}

	private CreditDrop(I18N locale, int value) {
		super(
				r -> locale.get("str/drop_content", locale.get("currency/cr", (500 * (r - 1)) + value * r)),
				(r, acc) -> acc.addCR((500L * (r - 1)) + (long) value * r, "Credit drop")
		);
	}
}
