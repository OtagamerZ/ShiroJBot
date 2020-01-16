package com.kuuhaku.model.jda;

public class Member {
	private final String id;
	private final String nickname;
	private final String avatar;

	public Member(String id, String nickname, String avatar) {
		this.id = id;
		this.nickname = nickname;
		this.avatar = avatar;
	}

	public String getId() {
		return id;
	}

	public String getNickname() {
		return nickname;
	}

	public String getAvatar() {
		return avatar;
	}
}
