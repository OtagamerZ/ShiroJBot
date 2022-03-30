/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.id;

import java.io.Serializable;
import java.util.Objects;

public class CompositeCoupleId implements Serializable {
	private String husbando;
	private String waifu;

	public CompositeCoupleId() {
	}

	public CompositeCoupleId(String husbando, String waifu) {
		this.husbando = husbando;
		this.waifu = waifu;
	}

	public String getHusbando() {
		return husbando;
	}

	public String getWaifu() {
		return waifu;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CompositeCoupleId that = (CompositeCoupleId) o;
		return Objects.equals(husbando, that.husbando) && Objects.equals(waifu, that.waifu);
	}

	@Override
	public int hashCode() {
		return Objects.hash(husbando, waifu);
	}
}
