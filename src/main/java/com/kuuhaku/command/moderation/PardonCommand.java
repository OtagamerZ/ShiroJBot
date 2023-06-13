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

import com.github.ygimenez.model.Page;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.model.persistent.id.WarnId;
import com.kuuhaku.model.persistent.user.Profile;
import com.kuuhaku.model.persistent.user.Warn;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;

import java.util.List;

@Command(
		name = "pardon",
		category = Category.MODERATION
)
@Signature({
		"<user:user:r> <id:number>",
		"<id:number:r> <id:number>"
})
public class PardonCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Member member;
		boolean useId = false;
		if (args.has("user")) {
			member = event.message().getMentions().getMembers().get(0);
		} else {
			useId = true;
			member = event.guild().getMemberById(args.getJSONArray("id").getString(0));
			if (member == null) {
				event.channel().sendMessage(locale.get("error/invalid_user")).queue();
				return;
			}
		}

		if ((useId && args.getJSONArray("id").size() < 2) || (!useId && !args.has("id"))) {
			Profile profile = DAO.find(Profile.class, new ProfileId(event.guild().getId(), member.getId()));
			if (profile.getWarns().isEmpty()) {
				event.channel().sendMessage(locale.get("error/no_warns")).queue();
				return;
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(locale.get("str/warns"));

			XStringBuilder sb = new XStringBuilder();
			List<Page> pages = Utils.generatePages(eb, profile.getWarns(), 20, 10,
					w -> {
						sb.clear();
						sb.appendNewLine("`ID: " + w.getId().getId() + "` " + w.getReason());
						sb.appendNewLine(locale.get("str/issuer", "<@" + w.getIssuer() + ">"));
						if (w.getPardoner() != null) {
							sb.appendNewLine(locale.get("str/pardoner", "<@" + w.getPardoner() + ">"));
						}

						return sb.toString();
					},
					(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
			);

			Utils.paginate(pages, 1, true, event.channel(), event.user());
			return;
		}

		Member self = event.guild().getSelfMember();
		if (member.equals(event.member())) {
			event.channel().sendMessage(locale.get("error/cant_pardon_yourself")).queue();
			return;
		} else if (!event.member().canInteract(member)) {
			event.channel().sendMessage(locale.get("error/cant_pardon_user")).queue();
			return;
		} else if (!self.canInteract(member)) {
			event.channel().sendMessage(locale.get("error/higher_hierarchy")).queue();
			return;
		}

		Warn w = DAO.find(Warn.class, new WarnId(
				useId ? args.getJSONArray("id").getInt(1) : args.getInt("id"),
				event.guild().getId(),
				member.getId())
		);

		if (w == null) {
			event.channel().sendMessage(locale.get("error/warn_not_found")).queue();
			return;
		}

		w.setPardoner(event.user());
		w.save();

		event.channel().sendMessage(locale.get("success/pardon")).queue();
	}
}
