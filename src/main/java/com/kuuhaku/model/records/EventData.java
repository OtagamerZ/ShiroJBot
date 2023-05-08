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

package com.kuuhaku.model.records;

import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.user.Profile;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public record EventData(MessageChannel channel, GuildConfig config, Profile profile) {
    public void notify(String message) {
        if (config.getSettings().isFeatureEnabled(GuildFeature.NOTIFICATIONS)) {
            GuildMessageChannel chn = config.getSettings().getNotificationsChannel();
            Utils.getOr(chn, channel).sendMessage(message).queue();
        }
    }
}
