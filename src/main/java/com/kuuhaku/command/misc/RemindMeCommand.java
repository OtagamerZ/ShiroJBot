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

import com.kuuhaku.Constants;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.SigPattern;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Reminder;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.schedule.HourlySchedule;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.StringUtils;

import java.time.temporal.ChronoField;

@Command(
		name = "remindme",
		category = Category.MISC
)
@Signature(
		patterns = @SigPattern(id = "duration", value = "(\\d+([dhmsDHMS])\\s*)+"),
		value = "<duration:custom:r>[duration] <message:text:r>"
)
public class RemindMeCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		long duration = Utils.stringToDuration(args.getString("duration"));
		if (duration < 5000) {
			event.channel().sendMessage(locale.get("error/invalid_value_low", 5 + " " + locale.get("str/second") + "s")).queue();
			return;
		}

		Reminder r = new Reminder(data.profile().getAccount(), event.channel(), args.getString("message"), duration);
		try {
			Utils.confirm(locale.get("question/reminder",
							StringUtils.abbreviate(r.getMessage(), 50),
							Constants.TIMESTAMP.formatted(r.getDue().getLong(ChronoField.INSTANT_SECONDS))
					), event.channel(), w -> {
						event.channel().sendMessage(locale.get("success/reminder")).queue();
						HourlySchedule.scheduleReminder(r.save());

						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
