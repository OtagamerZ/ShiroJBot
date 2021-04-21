/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common;

import com.kuuhaku.utils.Helper;
import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;

public class StockValue {
	private final String id;
	private final String name;
	private final int value;
	private final double growth;

	public StockValue(String id, String name, double[] before, double[] now) {
		this.id = id;
		this.name = name;

		GeometricMean gm = new GeometricMean();
		this.value = (int) Math.round(gm.evaluate(now));
		if (before.length == 0) this.growth = 0;
		else
			this.growth = Helper.mirroredFloor((Helper.prcnt(gm.evaluate(now), gm.evaluate(before)) * 1000) - 1) / 1000d;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getValue() {
		return value;
	}

	public double getGrowth() {
		return growth;
	}
}
