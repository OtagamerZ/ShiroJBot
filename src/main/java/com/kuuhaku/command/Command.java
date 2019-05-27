package com.kuuhaku.command;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public abstract class Command {

	private String name;
	private String[] aliases;
	private String usage;
	private String description;
	private Category category;
	
	public Command(String name, String description, Category category) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = null;
		this.description = description;
		this.category = category;
		this.category.addCommand(this);
	}
	
	public Command(String name, String[] aliases, String description, Category category) {
		this.name = name;
		this.aliases = aliases;
		this.usage = null;
		this.description = description;
		this.category = category;
		this.category.addCommand(this);
	}
	
	public Command(String name, String usage, String description, Category category) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = usage;
		this.description = description;
		this.category = category;
		this.category.addCommand(this);
	}
	
	public Command(String name, String[] aliases, String usage, String description, Category category) {
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
	public abstract void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix);
	
}
