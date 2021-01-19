/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.managers.Argument;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Category {
	DEV(ShiroInfo.getLocale(I18n.PT).getString("cat_dev-name"), "<:dev:674261700333142046>", "674261700333142046", ShiroInfo.getLocale(I18n.PT).getString("cat_dev-description"), PrivilegeLevel.DEV),
	SUPPORT(ShiroInfo.getLocale(I18n.PT).getString("cat_support-name"), "<:sheriff:674261700538662913>", "674261700538662913", ShiroInfo.getLocale(I18n.PT).getString("cat_support-description"), PrivilegeLevel.SUPPORT),
	MODERACAO(ShiroInfo.getLocale(I18n.PT).getString("cat_moderation-name"), "<:mod:674261700844716082>", "674261700844716082", ShiroInfo.getLocale(I18n.PT).getString("cat_moderation-description"), PrivilegeLevel.MOD),
	BETA(ShiroInfo.getLocale(I18n.PT).getString("cat_beta-name"), "<:beta:674261701109219328>", "674261701109219328", ShiroInfo.getLocale(I18n.PT).getString("cat_beta-description"), PrivilegeLevel.USER),
	FUN(ShiroInfo.getLocale(I18n.PT).getString("cat_fun-name"), "<:fun:674261700941185035>", "674261700941185035", ShiroInfo.getLocale(I18n.PT).getString("cat_fun-description"), PrivilegeLevel.USER),
	MISC(ShiroInfo.getLocale(I18n.PT).getString("cat_misc-name"), "<:misc:674261700354113536>", "674261700354113536", ShiroInfo.getLocale(I18n.PT).getString("cat_misc-description"), PrivilegeLevel.USER),
	INFO(ShiroInfo.getLocale(I18n.PT).getString("cat_info-name"), "<:info:674261700643651645>", "674261700643651645", ShiroInfo.getLocale(I18n.PT).getString("cat_info-description"), PrivilegeLevel.USER),
	MUSICA(ShiroInfo.getLocale(I18n.PT).getString("cat_music-name"), "<:music:674261701507678220>", "674261701507678220", ShiroInfo.getLocale(I18n.PT).getString("cat_music-description"), PrivilegeLevel.USER),
	EXCEED(ShiroInfo.getLocale(I18n.PT).getString("cat_exceed-name"), "<:exceed:674261700312170496>", "674261700312170496", ShiroInfo.getLocale(I18n.PT).getString("cat_exceed-description"), PrivilegeLevel.USER),
	CLAN(ShiroInfo.getLocale(I18n.PT).getString("cat_clan-name"), "<:exceed:674261700312170496>", "674261700312170496", ShiroInfo.getLocale(I18n.PT).getString("cat_clan-description"), PrivilegeLevel.USER),
	NSFW(ShiroInfo.getLocale(I18n.PT).getString("cat_nsfw-name"), "<:nsfw:687649035204558894>", "687649035204558894", ShiroInfo.getLocale(I18n.PT).getString("cat_nsfw-description"), PrivilegeLevel.USER);

	private final String name;
	private final String emote;
	private final String emoteId;
	private final String description;
	private final PrivilegeLevel privilegeLevel;

	Category(String name, String emote, String emoteId, String description, PrivilegeLevel privilegeLevel) {
		this.name = name;
		this.emote = emote;
		this.emoteId = emoteId;
		this.description = description;
		this.privilegeLevel = privilegeLevel;
	}

	public boolean equals(Category other) {
		return this.getName().equals(other.getName());
	}
	
	public String getName() {
		return name;
	}

	public static Category getByName(String name) throws ArrayIndexOutOfBoundsException {
		return Arrays.stream(Category.values()).filter(c -> Helper.equalsAny(name, StringUtils.stripAccents(c.name), c.name, c.name())).findFirst().orElse(null);
	}

	public String getDescription() {
		return description;
	}

	public PrivilegeLevel getPrivilegeLevel() {
		return privilegeLevel;
	}

	public List<Argument> getCmds() {
		return Main.getCommandManager().getCommands().values().stream().filter(a -> a.getCategory().equals(this)).collect(Collectors.toList());
	}

	public boolean isEnabled(GuildConfig gc, Guild g, User u) {
		if (this == NSFW) return false;
		else if ((this == DEV || this == SUPPORT) && (!g.getId().equals(ShiroInfo.getSupportServerID()) && !ShiroInfo.getDevelopers().contains(u.getId()))) {
			return false;
		} else if (this == BETA && (!TagDAO.getTagById(g.getOwnerId()).isBeta() && !ShiroInfo.getStaff().contains(u.getId()))) {
			return false;
		} else return !gc.getDisabledModules().contains(name());
	}

	public String getEmote() {
		return emote;
	}

	public String getEmoteId() {
		return emoteId;
	}
}
