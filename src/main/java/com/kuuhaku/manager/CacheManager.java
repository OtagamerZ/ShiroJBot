package com.kuuhaku.manager;

import com.kuuhaku.Constants;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;
import java.time.Duration;
import java.util.function.BiFunction;

public class CacheManager {
	private final File folder = new File("cache");
	private final org.ehcache.CacheManager cm = CacheManagerBuilder.newCacheManagerBuilder()
			.with(CacheManagerBuilder.persistence(folder))
			.build(true);

	private final Cache<String, byte[]> resource = cm.createCache("resource",
			CacheConfigurationBuilder
					.newCacheConfigurationBuilder(
							String.class, byte[].class,
							ResourcePoolsBuilder.newResourcePoolsBuilder()
									.heap(32, EntryUnit.ENTRIES)
									.offheap(512, MemoryUnit.MB)
									.disk(2, MemoryUnit.GB)

					)
					.withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(30)))
	);
	private final Cache<String, String> locale = cm.createCache("locale",
			CacheConfigurationBuilder
					.newCacheConfigurationBuilder(String.class, String.class, ResourcePoolsBuilder.heap(128))
					.withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMinutes(30)))
	);

	public CacheManager() {
		if (!folder.exists() && !folder.mkdir()) {
			Constants.LOGGER.fatal("Failed to create cache directory");
			System.exit(1);
		}
	}

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
