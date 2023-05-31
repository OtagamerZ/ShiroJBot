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

package com.kuuhaku.command.moderation;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

@Command(
		name = "antiraid",
		category = Category.MODERATION
)
@Signature("<threshold:number>")
@Requires({Permission.BAN_MEMBERS, Permission.MANAGE_PERMISSIONS, Permission.MANAGE_CHANNEL})
public class AntiRaidCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
        GuildSettings settings = data.config().getSettings();
        if (args.has("value")) {
			int thr = args.getInt("value");
			if (!Utils.between(thr, 100, 2000)) {
				event.channel().sendMessage(locale.get("error/invalid_value_range", 100, 2000)).queue();
				return;
			}

			settings.setAntiRaidThreshold(thr);

			String msg = locale.get("success/anti_raid_threshold", thr);
			if (!settings.isFeatureEnabled(GuildFeature.ANTI_RAID)) {
				msg += "\n" + locale.get("success/anti_raid_enable");
			}

			event.channel().sendMessage(msg).queue();
		} else {
			if (settings.isFeatureEnabled(GuildFeature.ANTI_RAID)) {
				settings.getFeatures().remove(GuildFeature.ANTI_RAID);
				event.channel().sendMessage(locale.get("success/anti_raid_disable")).queue();
			} else {
				settings.getFeatures().add(GuildFeature.ANTI_RAID);
				event.channel().sendMessage(locale.get("success/anti_raid_enable")).queue();
			}
		}

        settings.save();
	}
}
