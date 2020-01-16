package com.kuuhaku.model.jda;

public abstract class Guild implements net.dv8tion.jda.api.entities.Guild {
	private final String id = getId();
	private final String name = getName();
	private final String icon = getIconUrl();
	private final Member[] members = (Member[]) getMembers().toArray(new net.dv8tion.jda.api.entities.Member[0]);

	public String getid() {
		return id;
	}

	public String getname() {
		return name;
	}

	public String geticon() {
		return icon;
	}

	public Member[] getmembers() {
		return members;
	}
}
