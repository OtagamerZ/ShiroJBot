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
@Table(name = "version")
public class Version {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int major;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int minor = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int build = 0;

	public Version(int major) {
		this.major = major;
	}

	public Version() {
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public int getBuild() {
		this.build++;
		return build;
	}
}
