package com.kuuhaku.model.jda;

public class Guild {
	private final String id;
	private final String name;
	private final String icon;

	public Guild(String id, String name, String icon) {
		this.id = id;
		this.name = name;
		this.icon = icon;
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
}
