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

package com.kuuhaku.command.misc;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.PseudoUser;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.text.Uwuifier;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "uwu",
		category = Category.MISC
)
@Signature("<text:text:r>")
public class UwuCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		PseudoUser pu = new PseudoUser(event.member(), event.channel());

		String text = Uwuifier.INSTANCE.uwu(locale, args.getString("text"));
		try (WebhookClient hook = pu.webhook()) {
			if (hook != null) {
				event.message().delete().queue(null, Utils::doNothing);
				WebhookMessage msg = new WebhookMessageBuilder()
						.setUsername(pu.name())
						.setAvatarUrl(pu.avatar())
						.setAllowedMentions(AllowedMentions.none())
						.setContent(text)
						.build();

				hook.send(msg);
			} else {
				event.channel().sendMessage(text).setMessageReference(event.message()).queue();
			}
		}
	}
}