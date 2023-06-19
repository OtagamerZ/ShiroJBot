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

package com.kuuhaku.game.engine;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.shoukan.HistoryLog;

import java.awt.image.BufferedImage;
import java.util.Deque;

public interface Renderer {
	default BufferedImage render(I18N locale) {
		throw new IllegalStateException("Not implemented");
	}

	default BufferedImage render(I18N locale, Deque<HistoryLog> history) {
		throw new IllegalStateException("Not implemented");
	}
}
