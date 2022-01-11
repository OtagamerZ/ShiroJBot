package com.kuuhaku.model.records;

import com.kuuhaku.model.persistent.MatchMakingRating;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.concurrent.atomic.AtomicBoolean;

public record SoloLobby(MatchMakingRating mmr, TextChannel channel, AtomicBoolean unlocked) {
	public SoloLobby(MatchMakingRating mmr, TextChannel channel) {
		this(mmr, channel, new AtomicBoolean());
	}
}
