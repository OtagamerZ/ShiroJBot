package com.kuuhaku.util;

import com.kuuhaku.Constants;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayDeque;
import java.util.HashMap;

public class Checkpoint implements AutoCloseable {
	private final StopWatch watch = new StopWatch();
	private final ArrayDeque<Long> laps = new ArrayDeque<>();
	private final HashMap<Integer, String> comments = new HashMap<>();

	public Checkpoint() {
		watch.start();
		Constants.LOGGER.info("Measurements started");
	}

	public void lap() {
		watch.stop();
		laps.add(watch.getTime());
		watch.reset();
		watch.start();

		Constants.LOGGER.info("Lap " + laps.size() + " marked at " + laps.getLast() + "ms");
	}

	public void lap(String comment) {
		watch.stop();
		laps.add(watch.getTime());
		comments.put(laps.size(), comment);
		watch.reset();
		watch.start();

		Constants.LOGGER.info("Lap " + laps.size() + " marked at " + laps.getLast() + "ms");
	}

	@Override
	public void close() {
		watch.stop();
		laps.add(watch.getTime());

		long total = laps.stream().mapToLong(l -> l).sum();
		Constants.LOGGER.info("Final lap marked at " + laps.getLast() + "ms");
		if (total == 0) {
			Constants.LOGGER.info("All laps took 0ms to complete");
			return;
		}

		int i = 0;
		StringBuilder sb = new StringBuilder("\nTotal time: " + total + "ms");
		for (Long lap : laps) {
			sb.append("\n%s: %sms (%s%%) %s".formatted(
					++i, lap, lap * 100 / total, comments.getOrDefault(i, "")
			));
		}

		Constants.LOGGER.info(sb.toString());
	}
}
