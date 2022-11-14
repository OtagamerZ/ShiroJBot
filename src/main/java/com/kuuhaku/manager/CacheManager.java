package com.kuuhaku.manager;

import com.kuuhaku.Constants;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CacheManager {
	private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(3);
	private final DB db = DBMaker.heapDB().make();

	private final HTreeMap<String, byte[]> cardCache = db.hashMap("card", Serializer.STRING, Serializer.BYTE_ARRAY)
			.expireAfterCreate(30, TimeUnit.MINUTES)
			.expireAfterGet(30, TimeUnit.MINUTES)
			.expireExecutor(exec)
			.expireExecutorPeriod(TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES))
			.expireStoreSize(Constants.GB)
			.counterEnable()
			.create();

	private final HTreeMap<String, byte[]> resourceCache = db.hashMap("resource", Serializer.STRING, Serializer.BYTE_ARRAY)
			.expireAfterCreate(30, TimeUnit.MINUTES)
			.expireAfterGet(30, TimeUnit.MINUTES)
			.expireExecutor(exec)
			.expireExecutorPeriod(TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES))
			.expireStoreSize(Constants.GB)
			.counterEnable()
			.create();

	private final HTreeMap<String, String> emoteCache = db.hashMap("emote", Serializer.STRING, Serializer.STRING).create();

	private final HTreeMap<String, String> localeCache = db.hashMap("locale", Serializer.STRING, Serializer.STRING)
			.expireAfterCreate(30, TimeUnit.MINUTES)
			.expireAfterGet(30, TimeUnit.MINUTES)
			.expireExecutor(exec)
			.expireExecutorPeriod(TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES))
			.expireStoreSize(128 * Constants.MB)
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

	public HTreeMap<String, String> getLocaleCache() {
		return localeCache;
	}
}
