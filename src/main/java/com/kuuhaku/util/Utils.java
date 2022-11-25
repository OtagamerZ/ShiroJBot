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

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingFunction;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.listener.GuildListener;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.PatternCache;
import com.kuuhaku.model.common.SimpleMessageListener;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.StashItem;
import com.kuuhaku.util.json.JSONArray;
import com.kuuhaku.util.json.JSONObject;
import groovy.lang.Binding;
import groovy.lang.Script;
import jakarta.persistence.NoResultException;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

public abstract class Utils {
	public static final Set<String> CONFIMATIONS = ConcurrentHashMap.newKeySet();

	public static String toStringDuration(I18N locale, long millis) {
		long days = millis / Constants.MILLIS_IN_DAY;
		millis %= Constants.MILLIS_IN_DAY;
		long hours = millis / Constants.MILLIS_IN_HOUR;
		millis %= Constants.MILLIS_IN_HOUR;
		long minutes = millis / Constants.MILLIS_IN_MINUTE;
		millis %= Constants.MILLIS_IN_MINUTE;
		long seconds = millis / Constants.MILLIS_IN_SECOND;
		seconds %= Constants.MILLIS_IN_SECOND;

		return Stream.of(
				days > 0 ? days + " " + locale.get("str/day") + (days != 1 ? "s" : "") : "",
				hours > 0 ? hours + " " + locale.get("str/hour") + (hours != 1 ? "s" : "") : "",
				minutes > 0 ? minutes + " " + locale.get("str/minute") + (minutes != 1 ? "s" : "") : "",
				seconds > 0 ? seconds + " " + locale.get("str/second") + (seconds != 1 ? "s" : "") : ""
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

	public static <T> T getOr(T get, T or) {
		if (get instanceof String s && s.isBlank()) return or;
		else return get == null ? or : get;
	}

	public static <T> T getOr(Callable<T> get, T or) {
		try {
			Object obj = get.call();
			if (obj instanceof String s && s.isBlank()) return or;
			else return obj == null ? or : get.call();
		} catch (Exception e) {
			return or;
		}
	}

	@SafeVarargs
	public static <T> T getOrMany(T get, T... or) {
		T out = null;

		for (T t : or) {
			out = getOr(get, t);
			if (out != null && !(out instanceof String s && s.isBlank())) break;
		}

		return out;
	}

	@SafeVarargs
	public static <T> T getOrMany(Callable<T> get, T... or) {
		T out = null;

		for (T t : or) {
			out = getOr(get, t);
			if (out != null && !(out instanceof String s && s.isBlank())) break;
		}

		return out;
	}

	@SafeVarargs
	public static <T> boolean containsAll(Collection<T> value, T... compareWith) {
		return containsAll(value.toArray(), compareWith);
	}

	@SafeVarargs
	public static <T> boolean containsAll(T[] value, T... compareWith) {
		return Arrays.stream(value).allMatch(t -> ArrayUtils.contains(compareWith, t));
	}

	@SafeVarargs
	public static <T> boolean containsAny(Collection<T> value, T... compareWith) {
		return containsAny(value.toArray(), compareWith);
	}

	@SafeVarargs
	public static <T> boolean containsAny(T[] value, T... compareWith) {
		return Arrays.stream(value).anyMatch(s -> ArrayUtils.contains(compareWith, s));
	}

	public static <T> boolean equalsAll(T value, Collection<T> compareWith) {
		return equalsAll(value, compareWith.toArray());
	}

	@SafeVarargs
	public static <T> boolean equalsAll(T value, T... compareWith) {
		if (value instanceof String s) {
			return Arrays.stream(compareWith).allMatch(v -> s.equalsIgnoreCase((String) v));
		}

		return Arrays.stream(compareWith).allMatch(value::equals);
	}

	public static <T> boolean equalsAny(T value, Collection<T> compareWith) {
		return equalsAny(value, compareWith.toArray());
	}

	@SafeVarargs
	public static <T> boolean equalsAny(T value, T... compareWith) {
		if (value instanceof String s) {
			return Arrays.stream(compareWith).anyMatch(v -> s.equalsIgnoreCase((String) v));
		}

		return Arrays.asList(compareWith).contains(value);
	}

	public static String roundToString(Object value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		return new DecimalFormat("#,##0" + (places > 0 ? "." : "") + StringUtils.repeat("#", places)).format(value);
	}

	public static Matcher regex(String text, @Language("RegExp") String regex) {
		return PatternCache.compile(regex).matcher(text);
	}

	public static Matcher regex(String text, Pattern pattern) {
		return pattern.matcher(text);
	}

	public static String extract(String text, @Language("RegExp") String regex) {
		Matcher m = regex(text, regex);
		if (m.find()) return m.group();
		else return null;
	}

	public static String extract(String text, Pattern pattern) {
		Matcher m = pattern.matcher(text);
		if (m.find()) return m.group();
		else return null;
	}

	public static String extract(String text, @Language("RegExp") String regex, int group) {
		Matcher m = regex(text, regex);
		if (m.find()) return m.group(group);
		else return null;
	}

	public static String extract(String text, Pattern pattern, int group) {
		Matcher m = pattern.matcher(text);
		if (m.find()) return m.group(group);
		else return null;
	}

	public static String extract(String text, @Language("RegExp") String regex, String group) {
		Matcher m = regex(text, regex);
		if (m.find()) return m.group(group);
		else return null;
	}

	public static String extract(String text, Pattern pattern, String group) {
		Matcher m = pattern.matcher(text);
		if (m.find()) return m.group(group);
		else return null;
	}

	public static JSONArray extractGroups(String text, @Language("RegExp") String regex) {
		JSONArray out = new JSONArray();
		Matcher m = regex(text, regex);

		while (m.find()) {
			for (int i = 0; i < m.groupCount(); i++) {
				String val = m.group(i + 1);
				if (val != null) {
					out.add(val);
				}
			}
		}

		return out;
	}

	public static JSONArray extractGroups(String text, Pattern pattern) {
		JSONArray out = new JSONArray();
		Matcher m = pattern.matcher(text);

		while (m.find()) {
			for (int i = 0; i < m.groupCount(); i++) {
				String val = m.group(i + 1);
				if (val != null) {
					out.add(val);
				}
			}
		}

		return out;
	}

	public static JSONObject extractNamedGroups(String text, @Language("RegExp") String regex) {
		JSONArray names = extractGroups(regex, "\\(\\?<([a-zA-Z][A-z\\d]*)>");
		JSONObject out = new JSONObject();
		Matcher m = regex(text, regex);

		while (m.find()) {
			for (Object name : names) {
				String val = m.group((String) name);
				if (val != null) {
					out.putIfAbsent((String) name, val);
				}
			}
		}

		return out;
	}

	public static JSONObject extractNamedGroups(String text, Pattern pattern) {
		JSONArray names = extractGroups(pattern.toString(), "\\(\\?<([a-zA-Z][A-z\\d]*)>");
		JSONObject out = new JSONObject();
		Matcher m = pattern.matcher(text);

		while (m.find()) {
			for (Object name : names) {
				String val = m.group((String) name);
				if (val != null) {
					out.putIfAbsent((String) name, val);
				}
			}
		}

		return out;
	}

	public static String underline(String text) {
		return text.replaceAll("([A-OR-XZa-or-xz])", "$1\u0332");
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

	public static <T> Page generatePage(EmbedBuilder eb, Collection<T> list, int itemsPerColumn, Function<T, String> mapper) {
		if (list.isEmpty()) return null;
		eb.clearFields();

		XStringBuilder sb = new XStringBuilder();
		List<List<T>> cols = chunkify(list, itemsPerColumn);
		for (List<T> col : cols) {
			sb.clear();

			for (T t : col) {
				String val = mapper.apply(t);
				if (val != null) {
					sb.appendNewLine(val);
				}
			}

			eb.addField(new MessageEmbed.Field(Constants.VOID, sb.toString(), true));
		}

		return new InteractPage(eb.build());
	}

	public static <T> List<Page> generatePages(EmbedBuilder eb, Collection<T> list, int itemsPerPage, int itemsPerColumn, Function<T, String> mapper) {
		return generatePages(eb, list, itemsPerPage, itemsPerColumn, mapper, (p, t) -> {
		});
	}

	public static <T> List<Page> generatePages(EmbedBuilder eb, Collection<T> list, int itemsPerPage, int itemsPerColumn, Function<T, String> mapper, BiConsumer<Integer, Integer> finisher) {
		List<Page> pages = new ArrayList<>();
		List<List<T>> chunks = chunkify(list, itemsPerPage);
		for (int i = 0; i < chunks.size(); i++) {
			finisher.accept(i, chunks.size());
			pages.add(generatePage(eb, chunks.get(i), itemsPerColumn, mapper));
		}

		return pages;
	}

	public static Message paginate(List<Page> pages, TextChannel channel, User... allowed) {
		return paginate(pages, 1, false, channel, allowed);
	}

	public static Message paginate(List<Page> pages, int skip, TextChannel channel, User... allowed) {
		return paginate(pages, skip, false, channel, allowed);
	}

	public static Message paginate(List<Page> pages, int skip, boolean fast, TextChannel channel, User... allowed) {
		Message msg = Pages.subGet(channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()));

		Pages.paginate(msg, pages, true, 1, TimeUnit.MINUTES, skip, fast, u ->
				Arrays.asList(allowed).contains(u)
		);

		return msg;
	}

	public static Message paginate(ThrowingFunction<Integer, Page> loader, TextChannel channel, User... allowed) {
		Message msg = Pages.subGet(channel.sendMessageEmbeds((MessageEmbed) loader.apply(0).getContent()));

		Pages.lazyPaginate(msg, loader, true, 1, TimeUnit.MINUTES, u ->
				Arrays.asList(allowed).contains(u)
		);

		return msg;
	}

	public static void lock(User... users) {
		for (User user : users) {
			CONFIMATIONS.add(user.getId());
		}
	}

	public static void unlock(User... users) {
		for (User user : users) {
			CONFIMATIONS.remove(user.getId());
		}
	}

	public static void confirm(String text, TextChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, User... allowed) throws PendingConfirmationException {
		confirm(text, null, channel, action, m -> {}, allowed);
	}

	public static void confirm(String text, TextChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, Consumer<Message> onCancel, User... allowed) throws PendingConfirmationException {
		confirm(text, null, channel, action, onCancel, allowed);
	}

	public static void confirm(MessageEmbed embed, TextChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, User... allowed) throws PendingConfirmationException {
		confirm(null, embed, channel, action, m -> {}, allowed);
	}

	public static void confirm(MessageEmbed embed, TextChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, Consumer<Message> onCancel, User... allowed) throws PendingConfirmationException {
		confirm(null, embed, channel, action, onCancel, allowed);
	}

	public static void confirm(String text, MessageEmbed embed, TextChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, User... allowed) throws PendingConfirmationException {
		confirm(text, embed, channel, action, m -> {}, allowed);
	}

	public static void confirm(String text, MessageEmbed embed, TextChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, Consumer<Message> onCancel, User... allowed) throws PendingConfirmationException {
		for (User user : allowed) {
			if (CONFIMATIONS.contains(user.getId())) throw new PendingConfirmationException();
		}

		lock(allowed);
		MessageAction ma;
		if (text == null) {
			ma = channel.sendMessageEmbeds(embed);
		} else {
			ma = channel.sendMessage(text);
			if (embed != null) {
				ma = ma.setEmbeds(embed);
			}
		}

		AtomicBoolean lock = new AtomicBoolean(false);
		ma.queue(s -> Pages.buttonize(s,
						Map.of(parseEmoji(Constants.ACCEPT), w -> {
							if (!lock.get() && action.apply(w)) {
								lock.set(true);
								w.getMessage().delete().queue(null, Utils::doNothing);
								unlock(allowed);
							}
						}), true, true, 1, TimeUnit.MINUTES,
						u -> Arrays.asList(allowed).contains(u),
						c -> onCancel.andThen(m -> unlock(allowed)).accept(c)
				)
		);
	}

	public static CompletableFuture<Message> awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act) {
		CompletableFuture<Message> result = new CompletableFuture<>();

		GuildListener.addHandler(chn.getGuild(), new SimpleMessageListener(chn) {
			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getAuthor().equals(u) && act.apply(event.getMessage())) {
					result.complete(event.getMessage());
					close();
				}
			}
		});

		return result;
	}

	public static CompletableFuture<Message> awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act, int time, TimeUnit unit) {
		CompletableFuture<Message> result = new CompletableFuture<>();

		GuildListener.addHandler(chn.getGuild(), new SimpleMessageListener(chn) {
			final ScheduledFuture<?> timeout = Executors.newSingleThreadScheduledExecutor().schedule(() -> {
				result.complete(null);
				close();
			}, time, unit);

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getAuthor().equals(u) && act.apply(event.getMessage())) {
					result.complete(event.getMessage());
					timeout.cancel(true);
					close();
				}
			}
		});

		return result;
	}

	public static <T> T getRandomEntry(Collection<T> col) {
		if (col.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");

		List<T> list = List.copyOf(col);
		if (list.size() == 1) return list.get(0);

		return list.get(Calc.rng(list.size() - 1));
	}

	@SafeVarargs
	public static <T> T getRandomEntry(T... array) {
		if (array.length == 0) throw new IllegalArgumentException("Array must not be empty");
		else if (array.length == 1) return array[0];

		List<T> list = List.of(array);

		return list.get(Calc.rng(list.size() - 1));
	}

	public static <T> T getRandomEntry(RandomGenerator random, Collection<T> col) {
		if (col.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");

		List<T> list = List.copyOf(col);
		if (list.size() == 1) return list.get(0);

		return list.get(Calc.rng(list.size() - 1, random));
	}

	@SafeVarargs
	public static <T> T getRandomEntry(RandomGenerator random, T... array) {
		if (array.length == 0) throw new IllegalArgumentException("Array must not be empty");
		else if (array.length == 1) return array[0];

		List<T> list = List.of(array);

		return list.get(Calc.rng(list.size() - 1, random));
	}

	public static <T> List<T> getRandomN(List<T> list, int elements) {
		List<T> aux = new ArrayList<>(list);
		List<T> out = new ArrayList<>();
		RandomGenerator random = Constants.DEFAULT_RNG.get();

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = Calc.rng(aux.size() - 1, random);

			out.add(aux.get(index));
			Collections.shuffle(aux);
		}

		return out;
	}

	public static <T> List<T> getRandomN(List<T> list, int elements, int maxInstances) {
		List<T> aux = new ArrayList<>(list);
		List<T> out = new ArrayList<>();
		RandomGenerator random = Constants.DEFAULT_RNG.get();

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = Calc.rng(aux.size() - 1, random);

			T inst = aux.get(index);
			if (Collections.frequency(out, inst) < maxInstances)
				out.add(inst);
			else {
				aux.remove(index);
				Collections.shuffle(aux);
				i--;
			}
		}

		return out;
	}

	public static <T> List<T> getRandomN(List<T> list, int elements, int maxInstances, long seed) {
		List<T> aux = new ArrayList<>(list);
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

	public static Emoji parseEmoji(Emote in) {
		return Emoji.fromEmote(in);
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

	public static boolean between(int val, int from, int to) {
		return val >= from && val <= to;
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

	public static <T extends Collection<String>> Function<T, String> properlyJoin(String connector) {
		return objs -> {
			List<String> ls = List.copyOf(objs);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ls.size(); i++) {
				if (i == ls.size() - 1 && ls.size() > 1) sb.append(" ").append(connector).append(" ");
				else if (i > 0) sb.append(", ");

				sb.append(ls.get(i));
			}

			return sb.toString();
		};
	}

	public static InputStream getImage(String link) throws IOException {
		return new URL(link).openStream();
	}

	public static String getRandomHexColor() {
		return "#%06x".formatted(Calc.rng(0xFFFFFF));
	}

	public static String getRandomHexColor(long seed) {
		return "#%06x".formatted(Calc.rng(0xFFFFFF, seed));
	}

	public static Color getRandomColor() {
		return Color.decode(getRandomHexColor());
	}

	public static Color getRandomColor(long seed) {
		return Color.decode(getRandomHexColor(seed));
	}

	public static Color textToColor(String text) {
		CRC32 crc = new CRC32();
		crc.update(text.getBytes(StandardCharsets.UTF_8));
		return Color.decode("#%06x".formatted(crc.getValue() & 0xFFFFFF));
	}

	public static String replaceTags(Member mb, Guild guild, String str) {
		Map<String, String> replacements = new HashMap<>() {{
			if (mb != null) {
				put("%user%", mb.getAsMention());
				put("%user.id%", mb.getId());
				put("%user.name%", mb.getEffectiveName());
				put("%user.tag%", mb.getUser().getAsTag());
			}

			if (guild != null) {
				GuildConfig config = DAO.find(GuildConfig.class, guild.getId());

				put("%guild%", guild.getName());
				put("%guild.users%", separate(guild.getMemberCount()));

				Member owner = guild.getOwner();
				if (owner != null) {
					put("%guild.owner%", owner.getAsMention());
					put("%guild.owner.id%", owner.getId());
					put("%guild.owner.name%", owner.getEffectiveName());
					put("%guild.owner.tag%", owner.getUser().getAsTag());
				}
			}

			put("%now%", Constants.TIMESTAMP.formatted(System.currentTimeMillis() / 1000));
		}};

		for (Map.Entry<String, String> rep : replacements.entrySet()) {
			str = str.replace(rep.getKey(), rep.getValue());
		}

		return str;
	}

	public static String separate(Object value) {
		try {
			Number n = value instanceof Number nb ? nb : NumberUtils.createNumber(String.valueOf(value));
			DecimalFormat df = new DecimalFormat();
			df.setGroupingSize(3);

			return df.format(n);
		} catch (NumberFormatException e) {
			return String.valueOf(value);
		}
	}

	public static CompletionStage<StashedCard> selectOption(I18N locale, TextChannel channel, Collection<StashedCard> cards, Card card, User user) {
		List<StashedCard> matches = cards.stream()
				.filter(sc -> sc.getCard().equals(card))
				.sorted(
						Comparator.comparing(StashedCard::getType)
								.thenComparing(StashedCard::getId, Comparator.reverseOrder())
								.thenComparing(sc -> sc.getCard().getId())
				).toList();

		if (matches.isEmpty()) return CompletableFuture.failedStage(new NoResultException());
		if (matches.size() == 1) return CompletableFuture.completedStage(matches.get(0));

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/choose_option"));

		AtomicInteger i = new AtomicInteger();
		List<Page> pages = generatePages(eb, matches, 10, 5, sc -> new StashItem(locale, sc).toString(i.getAndIncrement()));

		Message msg = paginate(pages, channel, user);

		CompletableFuture<StashedCard> out = new CompletableFuture<>();
		awaitMessage(user, channel,
				m -> {
					try {
						int indx = Integer.parseInt(m.getContentRaw());
						out.complete(matches.get(indx));
					} catch (RuntimeException e) {
						out.complete(null);
					}

					msg.delete().queue(null, Utils::doNothing);
					return true;
				}, 1, TimeUnit.MINUTES
		);

		return out;
	}

	public static String didYouMean(String word, String... options) {
		String match = "";
		int threshold = 999;
		LevenshteinDistance checker = new LevenshteinDistance();

		for (String w : options) {
			if (word.equalsIgnoreCase(w)) {
				return word;
			} else {
				int diff = checker.apply(word.toLowerCase(), w.toLowerCase());
				if (diff < threshold) {
					match = w;
					threshold = diff;
				}
			}
		}

		return match;
	}

	public static Pair<String, Double> didYouMean(String word, Collection<String> options) {
		String match = "";
		int threshold = Integer.MAX_VALUE;
		LevenshteinDistance checker = new LevenshteinDistance();

		for (String w : options) {
			if (word.equalsIgnoreCase(w)) {
				return new Pair<>(w, 100d);
			} else {
				int diff = checker.apply(word.toLowerCase(), w.toLowerCase());
				if (diff < threshold) {
					match = w;
					threshold = diff;
				}
			}
		}

		int size = Math.max(word.length(), match.length());
		if (size == 0) return new Pair<>(match, 0d);
		else return new Pair<>(match, (size - threshold) * 100d / size);
	}

	public static Pair<CommandLine, Options> getCardCLI(I18N locale, String[] args, boolean market) {
		String[] longOp = {"name", "rarity", "anime", "chrome", "kawaipon", "evogear", "field", "valid", "min", "max", "mine"};
		String[] shortOp = {"n", "r", "a", "c", "k", "e", "f", "v", "gt", "lt", "m"};

		Options opt = new Options();
		for (int i = 0; i < longOp.length; i++) {
			if (market && i == 8) continue;
			else if (!market && i > 8) break;

			String lOp = longOp[i];
			String sOp = shortOp[i];
			opt.addOption(sOp, lOp, "nragtlt".contains(sOp), locale.get("search/" + lOp));
		}

		DefaultParser parser = new DefaultParser(false);
		try {
			return new Pair<>(parser.parse(opt, args, true), opt);
		} catch (ParseException e) {
			return new Pair<>(new CommandLine.Builder().build(), opt);
		}
	}

	public static <T> T fromNumber(Class<T> klass, Number n) {
		if (!Number.class.isAssignableFrom(klass)) throw new ClassCastException();

		if (klass == Short.class) {
			return klass.cast(n.shortValue());
		} else if (klass == Integer.class) {
			return klass.cast(n.intValue());
		} else if (klass == Long.class) {
			return klass.cast(n.longValue());
		} else if (klass == Float.class) {
			return klass.cast(n.floatValue());
		} else if (klass == Double.class) {
			return klass.cast(n.doubleValue());
		} else if (klass == Byte.class) {
			return klass.cast(n.byteValue());
		}

		throw new ClassCastException();
	}

	public static int toInt(boolean val) {
		return val ? 1 : 0;
	}

	public static <T> List<T> generate(int amount, Function<Integer, T> generator) {
		List<T> out = new ArrayList<>();
		for (int i = 0; i < amount; i++) {
			out.add(generator.apply(i));
		}

		return out;
	}

	public static String sign(Number n) {
		if (n.doubleValue() > 0) return "+" + n;

		return String.valueOf(n);
	}

	public static <T> Collector<T, ?, List<T>> toShuffledList() {
		return Collectors.collectingAndThen(
				Collectors.toList(),
				l -> {
					Collections.shuffle(l);
					return l;
				}
		);
	}

	@SafeVarargs
	public static <T> T getNext(T current, T... sequence) {
		return getNext(current, List.of(sequence));
	}

	@SafeVarargs
	public static <T> T getNext(T current, boolean wrap, T... sequence) {
		return getNext(current, wrap, List.of(sequence));
	}

	public static <T> T getNext(T current, List<T> sequence) {
		boolean found = false;
		for (T other : sequence) {
			if (found) return other;
			found = Objects.equals(current, other);
		}

		return null;
	}

	public static <T> T getNext(T current, boolean wrap, List<T> sequence) {
		T out = getNext(current, sequence);
		if (wrap) out = sequence.get(0);

		return out;
	}

	@SafeVarargs
	public static <T> T getPrevious(T current, T... sequence) {
		return getPrevious(current, List.of(sequence));
	}

	@SafeVarargs
	public static <T> T getPrevious(T current, boolean wrap, T... sequence) {
		return getPrevious(current, wrap, List.of(sequence));
	}

	public static <T> T getPrevious(T current, List<T> sequence) {
		boolean found = false;
		for (int i = sequence.size() - 1; i >= 0; i--) {
			T other = sequence.get(i);
			if (found) return other;
			found = Objects.equals(current, other);
		}

		return null;
	}

	public static <T> T getPrevious(T current, boolean wrap, List<T> sequence) {
		T out = getPrevious(current, sequence);
		if (wrap) out = sequence.get(sequence.size() - 1);

		return out;
	}

	public static String lPad(Object o, int pad) {
		return lPad(o, pad, null);
	}

	public static String lPad(Object o, int pad, String padChar) {
		return StringUtils.leftPad(String.valueOf(o), pad, padChar);
	}

	public static Object exec(@Language("Groovy") String code) {
		return exec(code, Map.of());
	}

	public static Object exec(@Language("Groovy") String code, Map<String, Object> variables) {
		Binding ctx = new Binding(variables);
		Script script = Constants.GROOVY.parse(code, ctx);

		return script.run();
	}

	public static Script compile(@Language("Groovy") String code) {
		return Constants.GROOVY.parse(code, new Binding());
	}

	public static <K, V> void shufflePairs(Map<K, V> map) {
		List<V> valueList = new ArrayList<V>(map.values());
		Collections.shuffle(valueList);
		Iterator<V> valueIt = valueList.iterator();

		for (Map.Entry<K, V> e : map.entrySet()) {
			e.setValue(valueIt.next());
		}
	}

	public static String shorten(double number) {
		if (number < 1000) return roundToString(number, 1);

		NavigableMap<Double, String> suffixes = new TreeMap<>() {{
			put(1_000D, "k");
			put(1_000_000D, "m");
			put(1_000_000_000D, "b");
			put(1_000_000_000_000D, "t");
			put(1_000_000_000_000_000D, "q");
		}};

		Map.Entry<Double, String> entry = suffixes.floorEntry(number);
		return roundToString(number / entry.getKey(), 1) + entry.getValue();
	}

	public static String replaceTags(String text, char delimiter, Map<String, Object> tags) {
		for (Map.Entry<String, Object> tag : tags.entrySet()) {
			text = text.replace(delimiter + tag.getKey() + delimiter, String.valueOf(tag.getValue()));
		}

		return text;
	}

	public static Webhook getWebhook(TextChannel channel) {
		List<Webhook> hooks = Pages.subGet(channel.retrieveWebhooks());
		for (Webhook hook : hooks) {
			if (Objects.equals(hook.getOwnerAsUser(), channel.getGuild().getSelfMember().getUser())) {
				return hook;
			}
		}

		try {
			return Pages.subGet(channel.createWebhook("Shiro"));
		} catch (PermissionException | ErrorResponseException e) {
			return null;
		}
	}

	public static boolean notNull(Object... objs) {
		for (Object obj : objs) {
			if (obj == null) return false;
		}

		return true;
	}

	public static String generateRandomHash(int length) {
		String method;

		if (length <= 0) return "";
		else if (length <= 32) method = "MD5";
		else if (length <= 40) method = "SHA-1";
		else if (length <= 64) method = "SHA-256";
		else if (length <= 128) method = "SHA-512";
		else return "";

		byte[] bytes = new byte[8];
		ThreadLocalRandom.current().nextBytes(bytes);

		String hash = Calc.hash(bytes, method);
		if (hash.isBlank()) return "";

		return hash.substring(0, length);
	}

	public static <In, Out> List<Out> map(Collection<In> in, Function<In, Out> mapper) {
		return in.stream().map(mapper).toList();
	}

	public static <In, Out> Out safeGet(In in, Function<In, Out> getter) {
		if (in == null) return null;

		try {
			return getter.apply(in);
		} catch (Exception e) {
			return null;
		}
	}
}
