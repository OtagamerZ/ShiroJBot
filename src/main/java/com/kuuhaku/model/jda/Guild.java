package com.kuuhaku.model.jda;

public class Guild {
	private final String id;
	private final String name;
	private final String icon;
	private final String owner;

	public Guild(String id, String name, String icon, String owner) {
		this.id = id;
		this.name = name;
		this.icon = icon;
		this.owner = owner;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getIcon() {
		return icon;
	}

	public String getOwner() {
		return owner;
	}

	@Override
	public String toString() {
		return id;
	}
}
