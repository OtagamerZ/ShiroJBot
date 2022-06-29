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

package com.kuuhaku.game.engine;

public class GameReport extends RuntimeException {
	public static final int SUCCESS = 0;
	public static final int INITIALIZATION_ERROR = 1;
	public static final int GAME_TIMEOUT = 2;

	private final int code;

	public GameReport(int code) {
		super("Report code " + code + ": " + getCodeName(code));
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static String getCodeName(int code) {
		return switch (code) {
			case SUCCESS -> "SUCCESS";
			case INITIALIZATION_ERROR -> "INITIALIZATION_ERROR";
			case GAME_TIMEOUT -> "GAME_TIMEOUT";
			default -> "UNKNOWN_CODE";
		};
	}
}
