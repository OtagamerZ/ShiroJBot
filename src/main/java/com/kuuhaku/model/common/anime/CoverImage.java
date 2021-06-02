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

package com.kuuhaku.model.common.anime;

import java.awt.*;

public class CoverImage {
	private String extraLarge;
	private String large;
	private String medium;
	private String color;

	public String getExtraLarge() {
		return extraLarge;
	}

	public void setExtraLarge(String value) {
		this.extraLarge = value;
	}

	public String getLarge() {
		return large;
	}

	public void setLarge(String value) {
		this.large = value;
	}

	public String getMedium() {
		return medium;
	}

	public void setMedium(String value) {
		this.medium = value;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String value) {
		this.color = value;
	}

	public Color getParsedColor() {
		return color == null ? null : Color.decode(color);
	}
}
