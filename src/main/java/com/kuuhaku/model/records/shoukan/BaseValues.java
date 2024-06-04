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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.model.common.SupplyChain;

import java.util.List;
import java.util.concurrent.Callable;

public record BaseValues(int hp, SupplyChain<Integer> mpGain, SupplyChain<Integer> handCapacity, int lifesteal) {
	public BaseValues() {
		this(6000, new SupplyChain<>(5), new SupplyChain<>(5), 0);
	}

	public BaseValues(Callable<List<?>> values) throws Exception {
		this(values.call());
	}

	@SuppressWarnings("unchecked")
	public BaseValues(List<?> values) {
		this(
				(int) values.get(0),
				(SupplyChain<Integer>) values.get(1),
				(SupplyChain<Integer>) values.get(2),
				(int) values.get(3)
		);
	}
}
