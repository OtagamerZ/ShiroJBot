package com.kuuhaku.model.jda;

public class Member {
	private final String id;
	private final String guild;
	private final String name;
	private final String nickname;
	private final String avatar;

	public Member(String id, String guild, String name, String nickname, String avatar) {
		this.id = id;
		this.guild = guild;
		this.name = name;
		this.nickname = nickname;
		this.avatar = avatar;
	}

	public String getId() {
		return id;
	}

	public String getGuild() {
		return guild;
	}

	public String getName() {
		return name;
	}

	public String getNickname() {
		return nickname;
	}

	public String getAvatar() {
		return avatar;
	}
}
