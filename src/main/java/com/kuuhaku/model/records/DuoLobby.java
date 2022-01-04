package com.kuuhaku.model.records;

import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public record DuoLobby(RankedDuo duo, TextChannel channel, AtomicInteger threshold, AtomicBoolean unlocked) {
	public DuoLobby(RankedDuo duo, TextChannel channel) {
		this(duo, channel, new AtomicInteger(), new AtomicBoolean());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DuoLobby soloLobby = (DuoLobby) o;
		return Objects.equals(duo, soloLobby.duo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(duo);
	}
}
