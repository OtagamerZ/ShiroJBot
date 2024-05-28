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

package com.kuuhaku.command.dev;

import com.kuuhaku.Main;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "clearcache",
		category = Category.STAFF
)
public class ClearCacheCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Main.getCacheManager().getResourceCache().cleanUp();
		Main.getCacheManager().getLocaleCache().cleanUp();
		Main.getCacheManager().getScriptCache().cleanUp();
		Main.getCacheManager().getPatternCache().cleanUp();

		event.channel().sendMessage(locale.get("success/cache_clear")).queue();
	}
}
