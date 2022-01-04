package com.kuuhaku.model.records;

import com.kuuhaku.model.persistent.MatchMakingRating;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public record SoloLobby(MatchMakingRating mmr, TextChannel channel, AtomicInteger threshold, AtomicBoolean unlocked) {
	public SoloLobby(MatchMakingRating mmr, TextChannel channel) {
		this(mmr, channel, new AtomicInteger(), new AtomicBoolean());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SoloLobby soloLobby = (SoloLobby) o;
		return Objects.equals(mmr, soloLobby.mmr);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mmr);
	}
}
