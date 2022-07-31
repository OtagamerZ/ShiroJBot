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
	public static final byte SUCCESS = 0;
	public static final byte INITIALIZATION_ERROR = 1;
	public static final byte GAME_TIMEOUT = 2;
	public static final byte NO_DECK = 3;
	public static final byte DICT_NOT_FOUND = 4;

	private final byte code;
	private final String content;

	public GameReport(byte code) {
		this(code, "");
	}

	public GameReport(byte code, String content) {
		super("Report code " + code + ": " + getCodeName(code));
		this.code = code;
		this.content = content;
	}

	public byte getCode() {
		return code;
	}

	public String getContent() {
		return content;
	}

	public static String getCodeName(int code) {
		return switch (code) {
			case SUCCESS -> "SUCCESS";
			case INITIALIZATION_ERROR -> "INITIALIZATION_ERROR";
			case GAME_TIMEOUT -> "GAME_TIMEOUT";
			case NO_DECK -> "NO_DECK";
			case DICT_NOT_FOUND -> "DICT_NOT_FOUND";
			default -> "UNKNOWN_CODE";
		};
	}
}
