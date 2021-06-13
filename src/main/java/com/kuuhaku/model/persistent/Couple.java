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

package com.kuuhaku.model.persistent;

import javax.persistence.*;

@Entity
@Table(name = "couple")
public class Couple {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String husbando = "";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String waifu = "";

	public Couple(String husbando, String waifu) {
		this.husbando = husbando;
		this.waifu = waifu;
	}

	public Couple() {
	}

	public int getId() {
		return id;
	}

	public String getHusbando() {
		return husbando;
	}

	public void setHusbando(String husbando) {
		this.husbando = husbando;
	}

	public String getWaifu() {
		return waifu;
	}

	public void setWaifu(String waifu) {
		this.waifu = waifu;
	}
}
