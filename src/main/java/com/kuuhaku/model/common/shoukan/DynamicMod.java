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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DynamicMod extends ValueMod {
	private final List<Supplier<Double>> suppliers = new ArrayList<>();

	protected DynamicMod(Supplier<Double> supplier) {
		this(null, supplier);
	}

	public DynamicMod(Drawable<?> source, Supplier<Double> supplier) {
		this(source, supplier, -1);
	}

	public DynamicMod(Drawable<?> source, Supplier<Double> supplier, int expiration) {
		super(source, 0, expiration);
		this.suppliers.add(supplier);
	}

	public void addSupplier(Supplier<Double> supplier) {
		suppliers.add(supplier);
	}

	@Override
	public double getValue() {
		return super.getValue() + suppliers.parallelStream().map(Supplier::get).reduce(0d, Double::sum);
	}
}
