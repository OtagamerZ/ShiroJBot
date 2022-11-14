package com.kuuhaku.manager;

import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;

public class CacheManager {
	private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(3);
	private final org.ehcache.CacheManager cm = CacheManagerBuilder.newCacheManagerBuilder().build(true);

	private final Cache<String, byte[]> resource = cm.createCache("resource",
			CacheConfigurationBuilder
					.newCacheConfigurationBuilder(String.class, byte[].class, ResourcePoolsBuilder.heap(100))
					.withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(30)))
	);
	private final Cache<String, String> locale = cm.createCache("locale",
			CacheConfigurationBuilder
					.newCacheConfigurationBuilder(String.class, String.class, ResourcePoolsBuilder.heap(100))
					.withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(30)))
	);

	public Cache<String, byte[]> getResourceCache() {
		return resource;
	}

	public byte[] computeResource(String key, BiFunction<String, byte[], byte[]> mapper) {
		byte[] bytes = mapper.apply(key, resource.get(key));
		resource.put(key, bytes);

		return bytes;
	}

	public Cache<String, String> getLocaleCache() {
		return locale;
	}

	public String computeLocale(String key, BiFunction<String, String, String> mapper) {
		String value = mapper.apply(key, locale.get(key));
		locale.put(key, value);

		return value;
	}
}
