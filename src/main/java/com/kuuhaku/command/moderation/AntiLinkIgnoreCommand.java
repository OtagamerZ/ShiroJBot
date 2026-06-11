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
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.AutoModType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.automod.AutoModRule;
import net.dv8tion.jda.internal.entities.RoleImpl;

import java.util.Set;

@Command(
		name = "antilink",
		path = "ignore",
		category = Category.MODERATION
)
@Syntax(allowEmpty = true, value = {
		"<action:word:r>[clear]",
		"<role:role:r>"
})
public class AntiLinkIgnoreCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();
		if (args.isEmpty()) {
			Set<RoleImpl> roles = settings.getLinkIgnoreRoles();
			if (roles.isEmpty()) {
				event.channel().sendMessage(locale.get("error/no_ignored_roles")).queue();
				return;
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(locale.get("str/link_ignored_roles"));

			Page page = Utils.generatePage(eb, roles, 10, RoleImpl::getAsMention);
			event.channel().sendMessageEmbeds((MessageEmbed) page.getContent()).queue();
			return;
		}

		if (args.has("action")) {
			settings.getLinkIgnoreRoles().clear();
			settings.save();

			event.channel().sendMessage(locale.get("success/link_ignore_role_clear")).queue();
		} else {
			Role role = event.roles(0);
			if (role == null) {
				event.channel().sendMessage(locale.get("error/invalid_mention")).queue();
				return;
			}

			if (settings.getLinkIgnoreRoles().stream().anyMatch(role::equals)) {
				settings.getLinkIgnoreRoles().removeIf(role::equals);
				event.channel().sendMessage(locale.get("success/link_ignore_role_remove", role.getAsMention())).queue();
			} else {
				if (!(role instanceof RoleImpl rc)) {
					event.channel().sendMessage(locale.get("error/invalid_role")).queue();
					return;
				} else if (settings.getLinkIgnoreRoles().size() >= AutoModRule.MAX_EXEMPT_ROLES) {
					event.channel().sendMessage(locale.get("error/link_ignore_role_max")).queue();
					return;
				}

				settings.getLinkIgnoreRoles().add(rc);
				event.channel().sendMessage(locale.get("success/link_ignore_role_add", role.getAsMention())).queue();
			}
		}

		String id = settings.getAutoModEntries().get(AutoModType.LINK);
		if (id != null) {
			event.guild().modifyAutoModRuleById(id)
					.setExemptRoles(settings.getLinkIgnoreRoles().toArray(new RoleImpl[0]))
					.queue();
		}

		settings.save();
	}
}
