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

package com.kuuhaku.command.misc;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Couple;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;

@Command(
		name = "marry",
		category = Category.MISC
)
@Signature("<user:user:r>")
public class MarryCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Couple c = data.profile().getAccount().getCouple();
		if (c != null) {
			event.channel().sendMessage(locale.get("error/already_married", c.getOther(event.user().getId()).getName())).queue();
			return;
		}

		Member other = event.message().getMentions().getMembers().get(0);
		if (DAO.query(Couple.class, "SELECT c FROM Couple c WHERE ?1 = c.id.first OR ?1 = c.id.second", other.getId()) != null) {
			event.channel().sendMessage(locale.get("error/already_married_target", other.getEffectiveName())).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/marry", other.getAsMention(), event.user().getAsMention()), event.channel(), w -> {
						new Couple(event.user().getId(), other.getId()).save();
						event.channel().sendMessage(locale.get("success/marry")).queue();
						return true;
					}, other.getUser()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}