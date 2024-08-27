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

package com.kuuhaku.command.deck;

import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.IO;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.utils.FileUpload;

@Command(
		name = "deck",
		category = Category.INFO
)
@Syntax("<private:word>[p]")
@Requires(Permission.MESSAGE_ATTACH_FILES)
public class DeckCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		if (args.containsKey("private")) {
			event.channel().sendMessage(Constants.LOADING.apply(locale.get("str/generating_image")))
					.flatMap(m -> event.user().openPrivateChannel()
							.flatMap(s -> s.sendFiles(FileUpload.fromData(IO.getBytes(d.render(locale), "png"), "deck.png")))
							.flatMap(s -> m.editMessage(locale.get("str/sent_in_private")))
					).queue();
		} else {
			event.channel().sendMessage(Constants.LOADING.apply(locale.get("str/generating_image")))
					.flatMap(m -> m.editMessage(event.user().getAsMention()).setFiles(FileUpload.fromData(IO.getBytes(d.render(locale), "png"), "deck.png")))
					.queue();
		}
	}
}