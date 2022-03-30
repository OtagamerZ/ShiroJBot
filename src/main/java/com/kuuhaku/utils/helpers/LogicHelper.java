package com.kuuhaku.utils.helpers;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

public abstract class LogicHelper {
	public static boolean compareWithValues(int value, int... compareWith) {
		return Arrays.stream(compareWith).anyMatch(v -> v == value);
	}

	public static boolean containsAll(String string, String... compareWith) {
		return Arrays.stream(compareWith).map(String::toLowerCase).allMatch(string.toLowerCase(Locale.ROOT)::contains);
	}

	public static boolean containsAny(String string, String... compareWith) {
		return Arrays.stream(compareWith).map(String::toLowerCase).anyMatch(string.toLowerCase(Locale.ROOT)::contains);
	}

	public static boolean containsAll(String[] string, String... compareWith) {
		return Arrays.stream(string).map(String::toLowerCase).allMatch(s -> ArrayUtils.contains(compareWith, s));
	}

	@SafeVarargs
	public static <T> boolean containsAll(T[] value, T... compareWith) {
		return Arrays.stream(value).allMatch(t -> ArrayUtils.contains(compareWith, t));
	}

	@SafeVarargs
	public static <T> boolean containsAll(Collection<T> value, T... compareWith) {
		return value.stream().allMatch(t -> ArrayUtils.contains(compareWith, t));
	}

	public static boolean containsAny(String[] string, String... compareWith) {
		return Arrays.stream(string).map(String::toLowerCase).anyMatch(s -> ArrayUtils.contains(compareWith, s));
	}

	@SafeVarargs
	public static <T> boolean containsAny(T[] value, T... compareWith) {
		return Arrays.stream(value).anyMatch(s -> ArrayUtils.contains(compareWith, s));
	}

	@SafeVarargs
	public static <T> boolean containsAny(Collection<T> value, T... compareWith) {
		return value.stream().anyMatch(s -> ArrayUtils.contains(compareWith, s));
	}

	public static boolean equalsAll(String string, String... compareWith) {
		return Arrays.stream(compareWith).allMatch(string::equalsIgnoreCase);
	}

	@SafeVarargs
	public static <T> boolean equalsAll(Object value, T... compareWith) {
		return Arrays.stream(compareWith).allMatch(value::equals);
	}

	public static boolean equalsAny(String string, String... compareWith) {
		return Arrays.stream(compareWith).anyMatch(string::equalsIgnoreCase);
	}

	@SafeVarargs
	public static <T> boolean equalsAny(T value, T... compareWith) {
		return Arrays.asList(compareWith).contains(value);
	}

	public static boolean notNull(Object... objs) {
		return Arrays.stream(objs).allMatch(Objects::nonNull);
	}

	public static boolean isEmpty(String... values) {
		boolean empty = false;
		for (String s : values) {
			if (s.isEmpty()) {
				empty = true;
				break;
			}
		}
		return empty;
	}
}
