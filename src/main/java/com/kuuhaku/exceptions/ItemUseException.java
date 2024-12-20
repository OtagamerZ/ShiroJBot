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

package com.kuuhaku.exceptions;

import java.io.Serial;

public class ItemUseException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = -5447658789385700637L;

	private final Object[] args;

	public ItemUseException(String key, Object... args) {
		super(key, null, true, false);
		this.args = args;
	}

	public Object[] getArgs() {
		return args;
	}
}
