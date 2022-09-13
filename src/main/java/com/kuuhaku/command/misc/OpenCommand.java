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

import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.Drop;
import com.kuuhaku.model.common.SingleUseReference;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Spawn;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "open",
		category = Category.MISC
)
@Signature("<captcha:word:r>")
public class OpenCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();

		SingleUseReference<Drop<?>> drop = Spawn.getSpawnedDrop(event.channel());
		try {
			if (!drop.isValid()) {
				event.channel().sendMessage(locale.get("error/no_drop")).queue();
				return;
			} else if (!drop.peekProperty(dp -> dp.check(acc))) {
				event.channel().sendMessage(locale.get("error/not_fulfilled")).queue();
				return;
			} else if (args.getString("captcha").contains(Constants.VOID)) {
				event.channel().sendMessage(locale.get("error/no_copy")).queue();
				return;
			} else if (!drop.peekProperty(dp -> args.getString("captcha").equalsIgnoreCase(dp.getCaptcha(false)))) {
				event.channel().sendMessage(locale.get("error/wrong_captcha")).queue();
				return;
			}
		} catch (NullPointerException e) {
			event.channel().sendMessage(locale.get("error/no_card")).queue();
			return;
		}

		Drop<?> dp = drop.get();
		dp.award(acc);

		event.channel().sendMessage(locale.get("success/claimed", event.user().getAsMention()))
				.setEmbeds(
						new EmbedBuilder()
								.setDescription(dp.getContent().toString())
								.build()
				)
				.queue();
	}
}
