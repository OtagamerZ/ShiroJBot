package com.kuuhaku.model.records;

import net.jodah.expiringmap.ExpiringMap;

import java.util.HashSet;
import java.util.Set;

public record RaidData(long start, Set<String> ids) {
	public RaidData(long start, ExpiringMap<Long, String> arc) {
		this(start, new HashSet<>());
		ids.addAll(arc.values());
	}
}
