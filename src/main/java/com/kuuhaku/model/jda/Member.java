package com.kuuhaku.model.jda;

public abstract class Member implements net.dv8tion.jda.api.entities.Member {
	private final String id = getId();
	private final String nickname = getNickname();
	private final String avatar = getUser().getAvatarUrl();

	public String getid() {
		return id;
	}

	public String getnickname() {
		return nickname;
	}

	public String getavatar() {
		return avatar;
	}
}
