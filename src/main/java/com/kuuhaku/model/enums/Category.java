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

package com.kuuhaku.model.enums;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.model.records.PreparedCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;

import java.util.Set;
import java.util.function.Function;

public enum Category {
	DEV("674261700333142046", Constants.DEV_PRIVILEGE),
	STAFF("674261700538662913", Constants.STF_PRIVILEGE),
	MODERATION("674261700844716082", Constants.MOD_PRIVILEGE),
	FUN("674261700962418688", Constants.USER_PRIVILEGE),
	MISC("674261700354113536", Constants.USER_PRIVILEGE),
	INFO("674261700643651645", Constants.USER_PRIVILEGE),
	CLAN("674261700941185035", Constants.USER_PRIVILEGE);

	private final String name;
	private final String description;
	private final String emote;
	private final Function<Member, Boolean> allowed;

	private Set<PreparedCommand> cmdCache = null;

	Category(String emote, Function<Member, Boolean> allowed) {
		this.name = "category/name/" + name().toLowerCase();
		this.description = "category/desc/" + name().toLowerCase();
		this.emote = emote;
		this.allowed = allowed;
	}

	public String getName(I18N loc) {
		return loc.get(name);
	}

	public String getDescription(I18N loc) {
		return loc.get(description);
	}

	public Set<PreparedCommand> getCommands() {
		if (cmdCache == null) {
			cmdCache = Main.getCommandManager().getCommands(this);
		}

		return cmdCache;
	}

	public CustomEmoji getEmote() {
		return Main.getApp().getShiro().getEmojiById(emote);
	}

	public boolean check(Member m) {
		return allowed.apply(m);
	}
}
