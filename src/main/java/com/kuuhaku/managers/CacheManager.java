package com.kuuhaku.managers;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CacheManager {
	private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
	private final DB db = DBMaker.memoryDB().make();

	private final HTreeMap<String, byte[]> cardCache = db.hashMap("card", Serializer.STRING, Serializer.BYTE_ARRAY)
			.expireAfterCreate(30, TimeUnit.MINUTES)
			.expireAfterGet(30, TimeUnit.MINUTES)
			.expireExecutor(exec)
			.expireExecutorPeriod(TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES))
			.counterEnable()
			.create();

	private final HTreeMap<String, byte[]> resourceCache = db.hashMap("resource", Serializer.STRING, Serializer.BYTE_ARRAY)
			.expireAfterCreate(30, TimeUnit.MINUTES)
			.expireAfterGet(30, TimeUnit.MINUTES)
			.expireExecutor(exec)
			.expireExecutorPeriod(TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES))
			.counterEnable()
			.create();

	private final HTreeMap<String, String> emoteCache = db.hashMap("emote", Serializer.STRING, Serializer.STRING)
			.counterEnable()
			.create();

	public HTreeMap<String, byte[]> getCardCache() {
		return cardCache;
	}

	public HTreeMap<String, byte[]> getResourceCache() {
		return resourceCache;
	}

	public HTreeMap<String, String> getEmoteCache() {
		return emoteCache;
	}
}
