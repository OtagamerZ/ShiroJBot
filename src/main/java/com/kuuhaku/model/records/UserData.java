package com.kuuhaku.model.records;

import net.dv8tion.jda.api.entities.User;

public record UserData(String uid, String name) {
	public UserData(User user) {
		this(user.getId(), user.getName());
	}
}
