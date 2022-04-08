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

package com.kuuhaku.utils;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.Language;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
	public static String toStringDuration(long millis) {
		long days = millis / Constants.MILLIS_IN_DAY;
		millis %= Constants.MILLIS_IN_DAY;
		long hours = millis / Constants.MILLIS_IN_HOUR;
		millis %= Constants.MILLIS_IN_HOUR;
		long minutes = millis / Constants.MILLIS_IN_MINUTE;
		millis %= Constants.MILLIS_IN_MINUTE;
		long seconds = millis / Constants.MILLIS_IN_SECOND;
		seconds %= Constants.MILLIS_IN_SECOND;

		return Stream.of(
				days > 0 ? days + " dia" + (days != 1 ? "s" : "") : "",
				hours > 0 ? hours + " hora" + (hours != 1 ? "s" : "") : "",
				minutes > 0 ? minutes + " minuto" + (minutes != 1 ? "s" : "") : "",
				seconds > 0 ? seconds + " segundo" + (seconds != 1 ? "s" : "") : ""
		).filter(s -> !s.isBlank()).collect(Collectors.collectingAndThen(Collectors.toList(), humanize(" e ")));
	}

	public static <T extends Collection<String>> Function<T, String> humanize(String last) {
		return objs -> {
			List<String> ls = List.copyOf(objs);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ls.size(); i++) {
				if (i == ls.size() - 1 && ls.size() > 1) sb.append(last);
				else if (i > 0) sb.append(", ");

				sb.append(ls.get(i));
			}

			return sb.toString();
		};
	}

	@SuppressWarnings("unchecked")
	public static <T> T getOr(Object get, T or) {
		if (get instanceof String s && s.isBlank()) return or;
		else return get == null ? or : (T) get;
	}

	@SafeVarargs
	public static <T> T getOrMany(Object get, T... or) {
		T out = null;

		for (T t : or) {
			out = getOr(get, t);
			if (out != null && !(out instanceof String s && s.isBlank())) break;
		}

		return out;
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

	public static String roundToString(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		return new DecimalFormat("#,##0" + (places > 0 ? "." : "") + StringUtils.repeat("#", places)).format(value);
	}

	public static Matcher regex(String text, @Language("RegExp") String regex) {
		return Pattern.compile(regex).matcher(text);
	}

	public static String extract(String text, @Language("RegExp") String regex) {
		Matcher m = Pattern.compile(regex).matcher(text);
		if (m.find()) return m.group();
		else return null;
	}

	public static String extract(String text, @Language("RegExp") String regex, int group) {
		Matcher m = Pattern.compile(regex).matcher(text);
		if (m.find()) return m.group(group);
		else return null;
	}

	public static List<String> extractGroups(String text, @Language("RegExp") String regex) {
		List<String> out = new ArrayList<>();
		Matcher m = Pattern.compile(regex).matcher(text);

		while (m.find()) {
			for (int i = 0; i < m.groupCount(); i++) {
				out.add(m.group(i + 1));
			}
		}

		return out.stream()
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	public static Map<String, String> extractNamedGroups(String text, @Language("RegExp") String regex) {
		List<String> names = extractGroups(regex, "\\(\\?<([a-zA-Z][A-z0-9]*)>");
		Map<String, String> out = new HashMap<>();
		Matcher m = Pattern.compile(regex).matcher(text);

		while (m.find()) {
			for (String name : names) {
				out.putIfAbsent(name, m.group(name));
			}
		}

		return out.entrySet().stream()
				.filter(e -> e.getValue() != null)
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public static String extract(String text, @Language("RegExp") String regex, String group) {
		Matcher m = Pattern.compile(regex).matcher(text);
		if (m.find()) return m.group(group);
		else return null;
	}

	public static String underline(String text) {
		return String.join("\u0332", text.split("")) + "\u0332";
	}

	public static <T> List<List<T>> chunkify(Collection<T> col, int chunkSize) {
		List<T> list = List.copyOf(col);
		int overflow = list.size() % chunkSize;
		List<List<T>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

		if (overflow > 0)
			chunks.add(list.subList(list.size() - overflow, list.size()));

		return chunks;
	}

	public static <T> List<List<T>> chunkify(List<T> list, int chunkSize) {
		int overflow = list.size() % chunkSize;
		List<List<T>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

		if (overflow > 0)
			chunks.add(list.subList(list.size() - overflow, list.size()));

		return chunks;
	}

	public static <T> List<List<T>> chunkify(Set<T> set, int chunkSize) {
		List<T> list = List.copyOf(set);
		int overflow = list.size() % chunkSize;
		List<List<T>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

		if (overflow > 0)
			chunks.add(list.subList(list.size() - overflow, list.size()));

		return chunks;
	}

	public static <K, V> List<List<Map.Entry<K, V>>> chunkify(Map<K, V> map, int chunkSize) {
		List<Map.Entry<K, V>> list = List.copyOf(map.entrySet());
		int overflow = list.size() % chunkSize;
		List<List<Map.Entry<K, V>>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

		if (overflow > 0)
			chunks.add(list.subList(list.size() - overflow, list.size()));

		return chunks;
	}

	public static <T> List<Page> generatePages(EmbedBuilder eb, List<T> list, int itemsPerPage, Function<T, MessageEmbed.Field> mapper) {
		List<Page> pages = new ArrayList<>();
		List<List<T>> chunks = chunkify(list, itemsPerPage);
		for (List<T> chunk : chunks) {
			eb.clearFields();

			for (T t : chunk) {
				eb.addField(mapper.apply(t));
			}

			pages.add(new InteractPage(eb.build()));
		}

		return pages;
	}

	public static void paginate(List<Page> pages, TextChannel channel, User... allowed) {
		paginate(pages, 1, false, channel, allowed);
	}

	public static void paginate(List<Page> pages, int skip, TextChannel channel, User... allowed) {
		paginate(pages, skip, false, channel, allowed);
	}

	public static void paginate(List<Page> pages, int skip, boolean fast, TextChannel channel, User... allowed) {
		channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, true, 1, TimeUnit.MINUTES, skip, fast, u ->
						Stream.of(allowed).anyMatch(a -> a.getId().equals(u.getId()))
				)
		);
	}

	public static void confirm(String text, TextChannel channel, ThrowingConsumer<ButtonWrapper> action, User... allowed) {
		channel.sendMessage(text).queue(s -> Pages.buttonize(s,
						Map.of(parseEmoji(Constants.ACCEPT), w -> {
							w.getMessage().delete().queue(null, Utils::doNothing);
							action.accept(w);
						}), true, true, 1, TimeUnit.MINUTES,
						u -> Stream.of(allowed).anyMatch(a -> a.getId().equals(u.getId()))
				)
		);
	}

	public static <T> T getRandomEntry(Collection<T> col) {
		if (col.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");
		List<T> list = List.copyOf(col);

		return list.get(Calc.rng(list.size() - 1));
	}

	@SafeVarargs
	public static <T> T getRandomEntry(T... array) {
		if (array.length == 0) throw new IllegalArgumentException("Array must not be empty");
		List<T> list = List.of(array);

		return list.get(Calc.rng(list.size() - 1));
	}

	public static <T> T getRandomEntry(Random random, Collection<T> col) {
		if (col.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");
		List<T> list = List.copyOf(col);

		return list.get(Calc.rng(list.size() - 1, random));
	}

	@SafeVarargs
	public static <T> T getRandomEntry(Random random, T... array) {
		if (array.length == 0) throw new IllegalArgumentException("Array must not be empty");
		List<T> list = List.of(array);

		return list.get(Calc.rng(list.size() - 1, random));
	}

	public static <T> List<T> getRandomN(List<T> array, int elements) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();
		Random random = new Random(System.currentTimeMillis());

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = Calc.rng(aux.size() - 1, random);

			out.add(aux.get(index));
			Collections.shuffle(aux, random);
		}

		return out;
	}

	public static <T> List<T> getRandomN(List<T> array, int elements, int maxInstances) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();
		Random random = new Random(System.currentTimeMillis());

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = Calc.rng(aux.size() - 1, random);

			T inst = aux.get(index);
			if (Collections.frequency(out, inst) < maxInstances)
				out.add(inst);
			else {
				aux.remove(index);
				Collections.shuffle(aux, random);
				i--;
			}
		}

		return out;
	}

	public static <T> List<T> getRandomN(List<T> array, int elements, int maxInstances, long seed) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();
		Random random = new Random(seed);

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = Calc.rng(aux.size() - 1, random);

			T inst = aux.get(index);
			if (Collections.frequency(out, inst) < maxInstances)
				out.add(inst);
			else {
				aux.remove(index);
				Collections.shuffle(aux, random);
				i--;
			}
		}

		return out;
	}

	public static Emoji parseEmoji(String in) {
		if (StringUtils.isNumeric(in)) {
			Emote e = Main.getApp().getShiro().getEmoteById(in);
			if (e == null) return Emoji.fromMarkdown("❓");

			return Emoji.fromEmote(e);
		}

		return Emoji.fromMarkdown(in);
	}

	public static void doNothing(Throwable thr) {

	}
}
