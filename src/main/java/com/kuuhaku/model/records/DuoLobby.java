package com.kuuhaku.model.records;

import net.dv8tion.jda.api.entities.TextChannel;

import java.util.concurrent.atomic.AtomicBoolean;

public record DuoLobby(RankedDuo duo, TextChannel channel, AtomicBoolean unlocked) {
	public DuoLobby(RankedDuo duo, TextChannel channel) {
		this(duo, channel, new AtomicBoolean());
	}
}
