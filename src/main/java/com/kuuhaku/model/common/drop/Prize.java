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

package com.kuuhaku.model.common.drop;

import net.dv8tion.jda.api.entities.User;

import java.util.Map;
import java.util.function.Function;

public interface Prize<P> {
	String getCaptcha();

	String getRealCaptcha();

	void award(User u);

	void awardInstead(User u, int prize);

	P getPrize();

	Map.Entry<String, Function<User, Boolean>> getRequirement();

	String toString(User u);
}
