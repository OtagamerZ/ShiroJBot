package com.kuuhaku.utils.collections;

import com.kuuhaku.utils.helpers.MiscHelper;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public class RefreshingMap<K, V> extends ConcurrentHashMap<K, V> implements Closeable {
	private final Callable<Map<K, V>> refresher;
	private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	public RefreshingMap(int initialCapacity, float loadFactor, Callable<Map<K, V>> refresher, int time, TimeUnit unit) {
		super(initialCapacity, loadFactor);
		this.refresher = refresher;
		exec.scheduleAtFixedRate(this::refresh, 0, time, unit);
	}

	public RefreshingMap(int initialCapacity, Callable<Map<K, V>> refresher, int time, TimeUnit unit) {
		super(initialCapacity);
		this.refresher = refresher;
		exec.scheduleAtFixedRate(this::refresh, 0, time, unit);
	}

	public RefreshingMap(Callable<Map<K, V>> refresher, int time, TimeUnit unit) {
		this.refresher = refresher;
		exec.scheduleAtFixedRate(this::refresh, 0, time, unit);
	}

	public RefreshingMap(Map<? extends K, ? extends V> m, Callable<Map<K, V>> refresher, int time, TimeUnit unit) {
		super(m);
		this.refresher = refresher;
		exec.scheduleAtFixedRate(this::refresh, 0, time, unit);
	}

	public void refresh() {
		clear();
		try {
			putAll(refresher.call());
		} catch (Exception e) {
			MiscHelper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}

	@Override
	public void close() throws IOException {
		exec.shutdown();
	}

	public boolean isClosed() {
		return exec.isShutdown();
	}
}
