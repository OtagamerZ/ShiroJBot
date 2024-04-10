package com.kuuhaku.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.openjdk.jol.vm.VM;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class CacheManager {
	private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
	private final Cache<String, byte[]> resource = Caffeine.newBuilder()
			.expireAfterAccess(30, TimeUnit.MINUTES)
			.maximumWeight(512 * 1024 * 1024)
			.weigher((k, v) -> (int) Math.min(VM.current().sizeOf(v), Integer.MAX_VALUE))
			.build();

	public Cache<String, byte[]> getResourceCache() {
		return resource;
	}

	public byte[] computeResource(String key, BiFunction<String, byte[], byte[]> mapper) {
		byte[] bytes = mapper.apply(key, resource.getIfPresent(key));
		if (bytes == null) return null;

		resource.put(key, bytes);
		return bytes;
	}
}
