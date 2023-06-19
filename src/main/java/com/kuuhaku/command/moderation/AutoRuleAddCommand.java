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
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.RuleAction;
import com.kuuhaku.model.persistent.guild.AutoRule;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

import java.util.List;

@Command(
		name = "autorule",
		path = "add",
		category = Category.MODERATION
)
@Signature("<threshold:number:r> <action:word:r>[mute,aggravate,lose_xp,delevel,kick,ban]")
public class AutoRuleAddCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();

		AutoRule rule = new AutoRule(settings,
				args.getInt("threshold"),
				args.getEnum(RuleAction.class, "action")
		);

		List<AutoRule> rules = settings.getAutoRules();
		if (rules.parallelStream().anyMatch(r -> r.getThreshold() == rule.getThreshold() && r.getAction() == rule.getAction())) {
			event.channel().sendMessage(locale.get("error/autorule_exists")).queue();
			return;
		}

		rules.add(rule);
		settings.save();

		event.channel().sendMessage(locale.get("success/autorule_add",
				locale.get("str/autorule_desc",
						locale.get("str/autorule_" + rule.getAction()),
						rule.getThreshold()
				)
		)).queue();
	}
}
