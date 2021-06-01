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

package com.kuuhaku.model.common.embed;

import com.kuuhaku.utils.Helper;

public class Image {
	private String image;
	private String join;
	private String leave;

	public String getImage() {
		return image;
	}

	public void setImage(String value) {
		this.image = value;
	}

	public String getJoin() {
		return Helper.getOr(join, image);
	}

	public void setJoin(String value) {
		this.join = value;
	}

	public String getLeave() {
		return Helper.getOr(leave, image);
	}

	public void setLeave(String value) {
		this.leave = value;
	}
}
