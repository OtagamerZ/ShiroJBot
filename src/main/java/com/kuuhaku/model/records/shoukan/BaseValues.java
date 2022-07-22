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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.interfaces.AccFunction;
import kotlin.Triple;

import java.util.concurrent.Callable;

public record BaseValues(int hp, AccFunction<Integer, Integer> mpGain, AccFunction<Integer, Integer> handCapacity) {
	public BaseValues() {
		this(5000, t -> 5, t -> 5);
	}

	public BaseValues(Callable<Triple<Integer, AccFunction<Integer, Integer>, AccFunction<Integer, Integer>>> values) throws Exception {
		this(values.call());
	}

	public BaseValues(Triple<Integer, AccFunction<Integer, Integer>, AccFunction<Integer, Integer>> values) {
		this(values.getFirst(), values.getSecond(), values.getThird());
	}
}
