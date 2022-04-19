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

package com.kuuhaku.command.moderation;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "nqnmode",
		category = Category.MODERATION
)
public class NqnModeCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
        GuildSettings settings = data.config().getSettings();
        if (settings.isFeatureEnabled(GuildFeature.NQN_MODE)) {
            settings.getFeatures().remove(GuildFeature.NQN_MODE);
            event.channel().sendMessage(locale.get("success/nqn_mode_disable")).queue();
        } else {
            settings.getFeatures().add(GuildFeature.NQN_MODE);
            event.channel().sendMessage(locale.get("success/nqn_mode_enable")).queue();
        }

        settings.save();
	}
}
