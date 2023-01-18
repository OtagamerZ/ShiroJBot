package com.kuuhaku.util;

import com.kuuhaku.Constants;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayDeque;

public class Checkpoint implements AutoCloseable {
	private final StopWatch watch = new StopWatch();
	private final ArrayDeque<Long> laps = new ArrayDeque<>();

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

	@Override
	public void close() {
		watch.stop();
		laps.add(watch.getTime());

		long total = laps.stream().mapToLong(l -> l).sum();
		Constants.LOGGER.info("Final lap marked at " + laps.getLast() + "ms");

		int i = 0;
		StringBuilder sb = new StringBuilder("\nTotal time: " + total + "ms");
		for (Long lap : laps) {
			sb.append("\n").append(++i).append(": ").append(lap).append("ms (").append(lap * 100 / total).append("%)");
		}

		Constants.LOGGER.info(sb.toString());
	}
}
