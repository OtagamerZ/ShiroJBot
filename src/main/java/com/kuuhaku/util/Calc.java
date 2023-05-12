/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.util;

import com.kuuhaku.Constants;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public abstract class Calc {
	public static int round(double value) {
		return (int) round(value, 0);
	}

	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		return Precision.round(value, places);
	}

	public static double clamp(double val, double min, double max) {
		return Math.max(min, Math.min(val, max));
	}

	public static float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(val, max));
	}

	public static long clamp(long val, long min, long max) {
		return Math.max(min, Math.min(val, max));
	}

	public static int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(val, max));
	}

	public static double[] clamp(double[] vals, double min, double max) {
		for (int i = 0; i < vals.length; i++) {
			vals[i] = clamp(vals[i], min, max);
		}

		return vals;
	}

	public static float[] clamp(float[] vals, float min, float max) {
		for (int i = 0; i < vals.length; i++) {
			vals[i] = clamp(vals[i], min, max);
		}

		return vals;
	}

	public static long[] clamp(long[] vals, long min, long max) {
		for (int i = 0; i < vals.length; i++) {
			vals[i] = clamp(vals[i], min, max);
		}

		return vals;
	}

	public static int[] clamp(int[] vals, int min, int max) {
		for (int i = 0; i < vals.length; i++) {
			vals[i] = clamp(vals[i], min, max);
		}

		return vals;
	}

	public static boolean between(int val, int from, int to) {
		return val >= from && val < to;
	}

	public static boolean between(float val, float from, float to) {
		return val >= from && val < to;
	}

	public static boolean between(long val, long from, long to) {
		return val >= from && val < to;
	}

	public static boolean between(double val, double from, double to) {
		return val >= from && val < to;
	}

	public static int rng(int max) {
		return rng(0, max, Constants.DEFAULT_RNG.get());
	}

	public static int rng(int max, long seed) {
		return rng(0, max, new SplittableRandom(seed));
	}

	public static int rng(int max, RandomGenerator random) {
		return rng(0, max, random);
	}

	public static int rng(int min, int max) {
		return rng(min, max, Constants.DEFAULT_RNG.get());
	}

	public static int rng(int min, int max, long seed) {
		return rng(min, max, new SplittableRandom(seed));
	}

	public static int rng(int min, int max, RandomGenerator random) {
		return (int) Math.round(min + random.nextDouble() * (max - min));
	}

	public static double rng(double max) {
		return rng(0, max, Constants.DEFAULT_RNG.get());
	}

	public static double rng(double max, long seed) {
		return rng(0, max, new SplittableRandom(seed));
	}

	public static double rng(double max, RandomGenerator random) {
		return rng(0, max, random);
	}

	public static double rng(double min, double max) {
		return rng(min, max, Constants.DEFAULT_RNG.get());
	}

	public static double rng(double min, double max, long seed) {
		return rng(min, max, new SplittableRandom(seed));
	}

	public static double rng(double min, double max, RandomGenerator random) {
		return min + random.nextDouble() * (max - min);
	}

	public static float prcnt(float value, float max) {
		return value / max;
	}

	public static double prcnt(double value, double max) {
		return value / max;
	}

	public static float prcnt(float value, float max, int round) {
		return new BigDecimal((value * 100) / max).setScale(round, RoundingMode.HALF_EVEN).floatValue();
	}

	public static double prcnt(double value, double max, int round) {
		return new BigDecimal((value * 100) / max).setScale(round, RoundingMode.HALF_EVEN).doubleValue();
	}

	public static int prcntToInt(float value, float max) {
		return prcntToInt(value, max, RoundingMode.HALF_EVEN);
	}

	public static int prcntToInt(float value, float max, RoundingMode mode) {
		return (int) switch (mode) {
			case UP, CEILING, HALF_UP -> Math.ceil((value * 100) / max);
			case DOWN, FLOOR, HALF_DOWN -> Math.floor((value * 100) / max);
			default -> Math.round((value * 100) / max);
		};
	}

	public static float offsetPrcnt(float value, float max, float offset) {
		return (value - offset) / (max - offset);
	}

	public static double offsetPrcnt(double value, double max, double offset) {
		return (value - offset) / (max - offset);
	}

	public static boolean chance(double percentage) {
		return chance(percentage, Constants.DEFAULT_RNG.get());
	}

	public static boolean chance(double percentage, RandomGenerator rng) {
		if (percentage >= 100) return true;
		else if (percentage <= 0) return false;

		return rng(100d, rng) < percentage;
	}

	public static <T> T getRandom(List<Pair<T, Double>> values) {
		EnumeratedDistribution<T> ed = new EnumeratedDistribution<>(
				values.stream()
						.sorted(Comparator.comparingDouble(Pair::getValue))
						.map(p -> p.getValue() < 0 ? Pair.create(p.getFirst(), 0d) : p)
						.collect(Collectors.toList())
		);

		return ed.sample();
	}

	public static double log(double value, double base) {
		return Math.log(value) / Math.log(base);
	}

	public static double[] normalize(double... values) {
		if (values.length < 2) return values;

		double max = DoubleStream.of(values).max().orElse(0);
		double min = DoubleStream.of(values).min().orElse(0);

		return DoubleStream.of(values)
				.map(d -> (d - min) / (max - min))
				.toArray();
	}

	public static double[] sumToOne(double... values) {
		if (values.length < 2) return values;

		double total = DoubleStream.of(values).sum();

		return DoubleStream.of(values)
				.map(d -> d / total)
				.toArray();
	}

	public static double mirroredCeil(double value) {
		return value < 0 ? Math.floor(value) : Math.ceil(value);
	}

	public static double mirroredFloor(double value) {
		return value > 0 ? Math.floor(value) : Math.ceil(value);
	}

	public static int average(int... values) {
		return Math.round(Arrays.stream(values).sum() / (float) values.length);
	}

	public static long average(long... values) {
		return Math.round(Arrays.stream(values).sum() / (double) values.length);
	}

	public static double average(double... values) {
		return Arrays.stream(values).average().orElse(0);
	}

	public static long getFibonacci(int nth) {
		if (nth <= 1) return nth;

		return getFibonacci(nth - 1) + getFibonacci(nth - 2);
	}

	public static int revFibonacci(int fib) {
		if (fib <= 1) return 2;

		return (int) log(fib * Math.sqrt(5) + 0.5, Constants.GOLDEN_RATIO);
	}

	public static double getRatio(double w, double h) {
		return w / h;
	}

	public static double luminance(Color color) {
		return luminance(color.getRGB());
	}

	public static double luminance(int[] rgb) {
		return luminance(rgb[0], rgb[1], rgb[2], rgb[3]);
	}

	public static double luminance(int r, int g, int b) {
		return luminance(255, r, g, b);
	}

	public static double luminance(int a, int r, int g, int b) {
		return luminance((a << 24) | (r << 16) | (g << 8) | b);
	}

	public static double luminance(int rgb) {
		return (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF)) / 255 * (((rgb >> 24) & 0xFF) / 255d);
	}

	public static int mult(int val, double[] add, double[] mult) {
		double a = 1;
		for (double v : add) {
			a += v;
		}

		double m = 1;
		for (double v : mult) {
			m *= v;
		}

		return (int) (val * a * m);
	}

	public static long mult(long val, double[] add, double[] mult) {
		double a = 1;
		for (double v : add) {
			a += v;
		}

		double m = 1;
		for (double v : mult) {
			m *= v;
		}

		return (long) (val * a * m);
	}

	public static float mult(float val, double[] add, double[] mult) {
		double a = 1;
		for (double v : add) {
			a += v;
		}

		double m = 1;
		for (double v : mult) {
			m *= v;
		}

		return (float) (val * a * m);
	}

	public static double mult(double val, double[] add, double[] mult) {
		double a = 1;
		for (double v : add) {
			a += v;
		}

		double m = 1;
		for (double v : mult) {
			m *= v;
		}

		return val * a * m;
	}

	public static String hash(byte[] bytes, String encoding) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance(encoding).digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			Constants.LOGGER.error(e, e);
			return "";
		}
	}

	public static String hash(String value, String encoding) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance(encoding).digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			Constants.LOGGER.error(e, e);
			return "";
		}
	}

	public static int interp(int min, int max, float fac) {
		return Math.round(min + (max - min) * fac);
	}

	public static long interp(long min, long max, double fac) {
		return Math.round(min + (max - min) * fac);
	}

	public static float interp(float min, float max, float fac) {
		return min + (max - min) * fac;
	}

	public static double interp(double min, double max, double fac) {
		return min + (max - min) * fac;
	}
}
