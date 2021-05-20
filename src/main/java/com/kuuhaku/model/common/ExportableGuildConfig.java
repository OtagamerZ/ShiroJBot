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

package com.kuuhaku.model.common;

import com.kuuhaku.Main;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.LevelRole;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExportableGuildConfig {
	private final JSONObject gc = new JSONObject();

	public ExportableGuildConfig(GuildConfig g) {
		gc.put("prefix", g.getPrefix());
		gc.put("welcomeChannel", new JSONObject() {{
			put("id", g.getWelcomeChannel().getId());

			TextChannel bv;
			try {
				bv = Main.getShiroShards().getTextChannelById(g.getWelcomeChannel().getId());
			} catch (IllegalArgumentException e) {
				bv = null;
			}
			if (bv != null) put("name", bv.getName());
		}});
		gc.put("welcomeMessage", g.getWelcomeMessage());
		gc.put("goodbyeChannel", new JSONObject() {{
			put("id", g.getByeChannel().getId());

			TextChannel ad = g.getByeChannel();
			if (ad != null) put("name", ad.getName());
		}});
		gc.put("goodbyeMessage", g.getByeMessage());
		gc.put("suggestionChannel", new JSONObject() {{
			put("id", g.getSuggestionChannel().getId());

			TextChannel sg = g.getSuggestionChannel();
			if (sg != null) put("name", sg.getName());
		}});
		gc.put("pollTime", g.getPollTime());
		gc.put("muteTime", g.getMuteTime());
		gc.put("muteRole", new JSONObject() {{
			put("id", g.getMuteRole().getId());

			Role r = g.getMuteRole();
			if (r != null) put("name", r.getName());
		}});
		gc.put("levelUpChannel", new JSONObject() {{
			put("id", g.getLevelChannel().getId());

			TextChannel lvl = g.getLevelChannel();
			if (lvl != null) put("name", lvl.getName());
		}});
		gc.put("levelRoles", new JSONArray() {{
			for (LevelRole role : g.getLevelRoles()) {
				put(new JSONObject() {{
					put("id", role.getId());

					Role r;
					try {
						r = Main.getInfo().getRoleByID(role.getId());
					} catch (IllegalArgumentException e) {
						r = null;
					}
					if (r != null) {
						put("name", r.getName());
						put("level", role.getLevel());
					}
				}});
			}
		}});
	}

	public JSONObject getGuildConfig() {
		return gc;
	}
}
