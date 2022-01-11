package com.kuuhaku.model.records;

import com.kuuhaku.model.persistent.MatchMakingRating;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public record SoloLobby(MatchMakingRating mmr, TextChannel channel, AtomicInteger threshold, AtomicBoolean unlocked) {
	public SoloLobby(MatchMakingRating mmr, TextChannel channel) {
		this(mmr, channel, new AtomicInteger(), new AtomicBoolean());
	}
}
