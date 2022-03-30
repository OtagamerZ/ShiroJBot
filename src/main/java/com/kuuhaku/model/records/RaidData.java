package com.kuuhaku.model.records;

import net.jodah.expiringmap.ExpiringMap;

import java.util.HashSet;
import java.util.Set;

public record RaidData(long start, Set<UserData> users) {
	public RaidData(long start, ExpiringMap<Long, UserData> arc) {
		this(start, new HashSet<>());
		users.addAll(arc.values());
	}
}
