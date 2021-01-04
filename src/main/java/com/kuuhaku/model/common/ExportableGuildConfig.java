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
import com.kuuhaku.model.persistent.GuildConfig;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class ExportableGuildConfig {
	private final JSONObject gc = new JSONObject();

	public ExportableGuildConfig(GuildConfig g) {
		gc.put("prefix", g.getPrefix());
		gc.put("welcomeChannel", new JSONObject() {{
			put("id", g.getCanalBV());

			TextChannel bv;
			try {
				bv = Main.getShiroShards().getTextChannelById(g.getCanalBV());
			} catch (IllegalArgumentException e) {
				bv = null;
			}
			if (bv != null) put("name", bv.getName());
		}});
		gc.put("welcomeMessage", g.getMsgBoasVindas());
		gc.put("goodbyeChannel", new JSONObject() {{
			put("id", g.getCanalAdeus());

			TextChannel ad;
			try {
				ad = Main.getShiroShards().getTextChannelById(g.getCanalAdeus());
			} catch (IllegalArgumentException e) {
				ad = null;
			}
			if (ad != null) put("name", ad.getName());
		}});
		gc.put("goodbyeMessage", g.getMsgAdeus());
		gc.put("suggestionChannel", new JSONObject() {{
			put("id", g.getCanalSUG());

			TextChannel sg;
			try {
				sg = Main.getShiroShards().getTextChannelById(g.getCanalSUG());
			} catch (IllegalArgumentException e) {
				sg = null;
			}
			if (sg != null) put("name", sg.getName());
		}});
		gc.put("pollTime", g.getPollTime());
		gc.put("muteTime", g.getWarnTime());
		gc.put("muteRole", new JSONObject() {{
			put("id", g.getCargoMute());

			Role r;
			try {
				r = Main.getInfo().getRoleByID(g.getCargoMute());
			} catch (IllegalArgumentException e) {
				r = null;
			}
			if (r != null) put("name", r.getName());
		}});
		gc.put("levelUpChannel", new JSONObject() {{
			put("id", g.getCanalLvl());

			TextChannel lvl;
			try {
				lvl = Main.getShiroShards().getTextChannelById(g.getCanalLvl());
			} catch (IllegalArgumentException e) {
				lvl = null;
			}
			if (lvl != null) put("name", lvl.getName());
		}});
		gc.put("levelRoles", new JSONArray() {{
			for (Map.Entry<String, Object> entry : g.getCargoslvl().entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				put(new JSONObject() {{
					put("id", String.valueOf(value));

					Role r;
					try {
						r = Main.getInfo().getRoleByID(String.valueOf(value));
					} catch (IllegalArgumentException e) {
						r = null;
					}
					if (r != null) {
						put("name", r.getName());
						put("level", key);
					}
				}});
			}
		}});
		gc.put("relayChannel", new JSONObject() {{
			put("id", g.getCanalRelay());

			TextChannel rl;
			try {
				rl = Main.getShiroShards().getTextChannelById(g.getCanalRelay());
			} catch (IllegalArgumentException e) {
				rl = null;
			}
			if (rl != null) put("name", rl.getName());
		}});
	}

	public JSONObject getGuildConfig() {
		return gc;
	}
}
