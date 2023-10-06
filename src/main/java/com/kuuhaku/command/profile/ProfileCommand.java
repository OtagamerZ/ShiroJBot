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

package com.kuuhaku.command.profile;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.model.persistent.user.Profile;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;

@Command(
		name = "profile",
		category = Category.MISC
)
@Signature(allowEmpty = true, value = "<user:user:r>")
@Requires(Permission.MESSAGE_ATTACH_FILES)
public class ProfileCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		event.channel().sendMessage(Constants.LOADING.apply(locale.get("str/generating_image"))).queue(m -> {
			User usr = Utils.getOr(event.users(0), event.user());
			if (usr.isBot()) {
				m.editMessage(locale.get("error/no_profile")).queue();
				return;
			}

			Profile p = DAO.find(Profile.class, new ProfileId(usr.getId(), event.guild().getId()));
			event.channel()
					.sendMessage(usr.getAsMention())
					.addFiles(FileUpload.fromData(IO.getBytes(p.render(locale), "png"), "profile.png"))
					.flatMap(s -> m.delete())
					.queue(null, Utils::doNothing);
		});
	}
}
