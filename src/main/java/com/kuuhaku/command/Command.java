/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public abstract class Command {

	private final String name;
	private final String[] aliases;
	private final String usage;
	private final String description;
	private final Category category;

	protected Command(String name, String description, Category category) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = null;
		this.description = description;
		this.category = category;
		this.category.addCommand(this);
	}

	protected Command(String name, String[] aliases, String description, Category category) {
		this.name = name;
		this.aliases = aliases;
		this.usage = null;
		this.description = description;
		this.category = category;
		this.category.addCommand(this);
	}

	protected Command(String name, String usage, String description, Category category) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = usage;
		this.description = description;
		this.category = category;
		this.category.addCommand(this);
	}

	protected Command(String name, String[] aliases, String usage, String description, Category category) {
		this.name = name;
		this.aliases = aliases;
		this.usage = usage;
		this.description = description;
		this.category = category;
		this.category.addCommand(this);
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
	
	public Category getCategory() {
		return category;
	}

	/**
     * @param  author
     *         O utilizador discord que executou o comando
     * @param  member
     *         O membro do servidor que executou o comando
     *
     */
	public abstract void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) throws Exception;
}
