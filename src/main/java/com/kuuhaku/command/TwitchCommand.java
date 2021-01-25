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

import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.enums.CommandPermission;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.common.events.domain.EventUser;
import com.kuuhaku.model.persistent.Account;
import org.jetbrains.annotations.NonNls;

import java.util.Set;

public abstract class TwitchCommand {

	private final String name;
	private final String[] aliases;
	private final String usage;
	private final String description;
	private final boolean requiresBinding;

	protected TwitchCommand(@NonNls String name, String description, boolean requiresBinding) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = null;
		this.description = description;
		this.requiresBinding = requiresBinding;
	}

	protected TwitchCommand(@NonNls String name, @NonNls String[] aliases, String description, boolean requiresBinding) {
		this.name = name;
		this.aliases = aliases;
		this.usage = null;
		this.description = description;
		this.requiresBinding = requiresBinding;
	}

	protected TwitchCommand(@NonNls String name, String usage, String description, boolean requiresBinding) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = usage;
		this.description = description;
		this.requiresBinding = requiresBinding;
	}

	protected TwitchCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, boolean requiresBinding) {
		this.name = name;
		this.aliases = aliases;
		this.usage = usage;
		this.description = description;
		this.requiresBinding = requiresBinding;
	}

	public String getName() {
		return name;
	}

	public String[] getAliases() {
		return aliases;
	}

	public String getUsage() {
		return usage;
	}

	public String getDescription() {
		return description;
	}

	public boolean requiresBinding() {
		return requiresBinding;
	}

	public abstract void execute(EventUser author, Account account, String command, String argsAsText, String[] args, ChannelMessageEvent message, EventChannel channel, TwitchChat chat, Set<CommandPermission> permissions);
}
