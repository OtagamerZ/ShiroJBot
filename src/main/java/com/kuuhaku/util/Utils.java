/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.github.ygimenez.model.*;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.listener.GuildListener;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.SimpleMessageListener;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shiro.ScriptMetrics;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.StashItem;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import groovy.lang.Binding;
import groovy.lang.Script;
import jakarta.persistence.NoResultException;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.intellij.lang.annotations.Language;
import org.jdesktop.swingx.graphics.ColorUtilities;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
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
		).filter(s -> !s.isBlank()).collect(Collectors.collectingAndThen(Collectors.toList(), properlyJoin(" e ")));
	}

	public static long stringToDuration(String input) {
		JSONObject groups = extractNamedGroups(input, "(?<days>\\d+)[dD]|(?<hours>\\d+)[hH]|(?<minutes>\\d+)[mM]|(?<seconds>\\d+)[sS]");

		long millis = 0;
		for (Map.Entry<String, Object> e : groups.entrySet()) {
			int val = Integer.parseInt(String.valueOf(e.getValue()));

			switch (e.getKey().toLowerCase()) {
				case "days" -> millis += val * Constants.MILLIS_IN_DAY;
				case "hours" -> millis += val * Constants.MILLIS_IN_HOUR;
				case "minutes" -> millis += val * Constants.MILLIS_IN_MINUTE;
				case "seconds" -> millis += val * Constants.MILLIS_IN_SECOND;
			}
		}

		return millis;
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
		Pattern pat = Main.getCacheManager().computePattern(regex, (k, v) -> v == null ? Pattern.compile(regex) : v);
		return pat.matcher(text);
	}

	public static Matcher regex(String text, Pattern pattern) {
		return pattern.matcher(text);
	}

	public static boolean match(String text, @Language("RegExp") String regex) {
		return regex(text, regex).matches();
	}

	public static boolean match(String text, Pattern pattern) {
		return regex(text, pattern).matches();
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
		return text.replaceAll("([A-OR-XZa-or-xz])", "$1̲");
	}

	public static <T> Page generatePage(EmbedBuilder eb, Collection<T> list, int itemsPerColumn, Function<T, String> mapper) {
		if (list.isEmpty()) return null;
		eb.clearFields();

		XStringBuilder sb = new XStringBuilder();
		List<List<T>> cols = ListUtils.partition(new ArrayList<>(list), itemsPerColumn);
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

		return InteractPage.of(eb.build());
	}

	public static <T> List<Page> generatePages(EmbedBuilder eb, Collection<T> list, int itemsPerPage, int itemsPerColumn, Function<T, String> mapper) {
		return generatePages(eb, list, itemsPerPage, itemsPerColumn, mapper, Utils::doNothing);
	}

	public static <T> List<Page> generatePages(EmbedBuilder eb, Collection<T> list, int itemsPerPage, int itemsPerColumn, Function<T, String> mapper, BiConsumer<Integer, Integer> finisher) {
		List<Page> pages = new ArrayList<>();
		List<List<T>> chunks = ListUtils.partition(new ArrayList<>(list), itemsPerPage);
		for (int i = 0; i < chunks.size(); i++) {
			finisher.accept(i, chunks.size());
			pages.add(generatePage(eb, chunks.get(i), itemsPerColumn, mapper));
		}

		return pages;
	}

	public static Message paginate(List<Page> pages, MessageChannel channel, User... allowed) {
		return paginate(pages, 1, false, channel, allowed);
	}

	public static Message paginate(List<Page> pages, int skip, MessageChannel channel, User... allowed) {
		return paginate(pages, skip, false, channel, allowed);
	}

	public static Message paginate(List<Page> pages, int skip, boolean fast, MessageChannel channel, User... allowed) {
		Message msg = Pages.subGet(sendPage(channel, pages.getFirst()));

		Pages.paginate(msg, pages, true, 1, TimeUnit.MINUTES, skip, fast, u ->
				Arrays.asList(allowed).contains(u)
		);

		return msg;
	}

	public static Message paginate(ThrowingFunction<Integer, Page> loader, MessageChannel channel, User... allowed) {
		Message msg = Pages.subGet(sendPage(channel, loader.apply(0)));

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

	public static CompletableFuture<Boolean> confirm(String text, MessageChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, User... allowed) throws PendingConfirmationException {
		return confirm(text, null, channel, action, Utils::doNothing, allowed);
	}

	public static CompletableFuture<Boolean> confirm(String text, MessageChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, Consumer<Message> onCancel, User... allowed) throws PendingConfirmationException {
		return confirm(text, null, channel, action, onCancel, allowed);
	}

	public static CompletableFuture<Boolean> confirm(MessageEmbed embed, MessageChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, User... allowed) throws PendingConfirmationException {
		return confirm(null, embed, channel, action, Utils::doNothing, allowed);
	}

	public static CompletableFuture<Boolean> confirm(MessageEmbed embed, MessageChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, Consumer<Message> onCancel, User... allowed) throws PendingConfirmationException {
		return confirm(null, embed, channel, action, onCancel, allowed);
	}

	public static CompletableFuture<Boolean> confirm(String text, MessageEmbed embed, MessageChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, User... allowed) throws PendingConfirmationException {
		return confirm(text, embed, channel, action, Utils::doNothing, allowed);
	}

	public static CompletableFuture<Boolean> confirm(String text, MessageEmbed embed, MessageChannel channel, ThrowingFunction<ButtonWrapper, Boolean> action, Consumer<Message> onCancel, User... allowed) throws PendingConfirmationException {
		for (User user : allowed) {
			if (CONFIMATIONS.contains(user.getId())) throw new PendingConfirmationException();
		}

		lock(allowed);
		MessageCreateAction ma;
		if (text == null) {
			ma = channel.sendMessageEmbeds(embed);
		} else {
			ma = channel.sendMessage(text);
			if (embed != null) {
				ma = ma.setEmbeds(embed);
			}
		}

		CompletableFuture<Boolean> lock = new CompletableFuture<>();
		ma.queue(s -> Pages.buttonize(s,
				Map.of(Utils.parseEmoji(Constants.ACCEPT), w -> {
					if (!lock.isDone() && action.apply(w)) {
						unlock(allowed);
						lock.complete(true);
						w.getMessage().delete().queue(null, Utils::doNothing);
					}
				}), true, true, 1, TimeUnit.MINUTES,
				u -> Arrays.asList(allowed).contains(u),
				c -> onCancel.andThen(m -> {
					unlock(allowed);
					lock.complete(false);
				}).accept(c)
		));

		return lock;
	}

	public static CompletableFuture<Message> awaitMessage(GuildMessageChannel chn, Function<Message, Boolean> act) {
		return awaitMessage(null, chn, act);
	}

	public static CompletableFuture<Message> awaitMessage(User u, GuildMessageChannel chn, Function<Message, Boolean> act) {
		return awaitMessage(u, chn, act, 0, null, null);
	}

	public static CompletableFuture<Message> awaitMessage(User u, GuildMessageChannel chn, Function<Message, Boolean> act, int time, TimeUnit unit, CompletableFuture<?> lock) {
		CompletableFuture<Message> result = new CompletableFuture<>();

		GuildListener.addHandler(chn.getGuild(), new SimpleMessageListener(chn) {
			private ScheduledFuture<?> timeout;

			{
				if (unit != null) {
					try (ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor()) {
						timeout = exec.schedule(() -> {
							if (lock != null) {
								lock.complete(null);
							}

							result.complete(null);
							close();
						}, time, unit);
					}
				}
			}

			@Override
			protected void onMessageReceived(@NotNull MessageReceivedEvent event) {
				if ((u == null || event.getAuthor().equals(u)) && act.apply(event.getMessage())) {
					if (timeout != null) {
						timeout.cancel(true);
						timeout = null;
					}

					result.complete(event.getMessage());
					close();
				}
			}
		});

		System.out.println(GuildListener.getHandler().keySet());

		return result;
	}

	public static <T> T getRandomEntry(Collection<T> col) {
		if (col.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");

		List<T> list = List.copyOf(col);
		if (list.size() == 1) return list.getFirst();

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
		if (list.size() == 1) return list.getFirst();

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

		for (int i = 0; i < elements && !aux.isEmpty(); i++) {
			int index = Calc.rng(aux.size() - 1, random);

			out.add(aux.get(index));
			Utils.shuffle(aux);
		}

		return out;
	}

	public static <T> List<T> getRandomN(List<T> list, int elements, int maxInstances) {
		return getRandomN(list, elements, maxInstances, new SplittableRandom());
	}

	public static <T> List<T> getRandomN(List<T> list, int elements, int maxInstances, long seed) {
		return getRandomN(list, elements, maxInstances, new SplittableRandom(seed));
	}

	public static <T> List<T> getRandomN(List<T> list, int elements, int maxInstances, RandomGenerator rng) {
		List<T> aux = new ArrayList<>(list);
		List<T> out = new ArrayList<>();

		for (int i = 0; i < elements && !aux.isEmpty(); i++) {
			int index = Calc.rng(aux.size() - 1, rng);

			T inst = aux.get(index);
			if (Collections.frequency(out, inst) < maxInstances) {
				out.add(inst);
			} else {
				aux.remove(index);
				i--;
			}

			Utils.shuffle(aux, rng);
		}

		return out;
	}

	public static Emoji parseEmoji(String in) {
		if (StringUtils.isNumeric(in)) {
			Emoji e = Main.getApp().getShiro().getEmojiById(in);
			if (e == null) return Utils.parseEmoji("❓");

			return e;
		}

		return Emoji.fromFormatted(in);
	}

	public static void doNothing(Object... ignored) {

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
			StringBuilder sb = new StringBuilder();

			int i = 0;
			int size = objs.size();
			for (String obj : objs) {
				if (i == size - 1 && size > 1) sb.append(" ").append(connector).append(" ");
				else if (i > 0) sb.append(", ");

				sb.append(obj);
				i++;
			}

			return sb.toString();
		};
	}

	public static InputStream getImage(String link) throws IOException {
		return URI.create(link).toURL().openStream();
	}

	public static String getRandomHexColor() {
		return "#%06x".formatted(Calc.rng(0xFFFFFF));
	}

	public static String getRandomHexColor(long seed) {
		return "#%06x".formatted(Calc.rng(0xFFFFFF, seed));
	}

	public static Color getRandomColor() {
		return ColorUtilities.HSLtoRGB((float) Calc.rng(1d), 0.8f, 0.5f);
	}

	public static Color getRandomColor(long seed) {
		return ColorUtilities.HSLtoRGB((float) Calc.rng(1d, seed), 0.8f, 0.5f);
	}

	public static Color textToColor(String text) {
		CRC32 crc = new CRC32();
		crc.update(text.getBytes(StandardCharsets.UTF_8));
		return Color.decode("#%06x".formatted(crc.getValue() & 0xFFFFFF));
	}

	public static String replaceTags(Member mb, Guild guild, String str) {
		Map<String, String> repls = new HashMap<>();
		if (mb != null) {
			repls.put("%user%", mb.getAsMention());
			repls.put("%user.id%", mb.getId());
			repls.put("%user.name%", mb.getEffectiveName());
		}

		if (guild != null) {
			repls.put("%guild%", guild.getName());
			repls.put("%guild.users%", separate(guild.getMemberCount()));

			Member owner = guild.getOwner();
			if (owner != null) {
				repls.put("%guild.owner%", owner.getAsMention());
				repls.put("%guild.owner.id%", owner.getId());
				repls.put("%guild.owner.name%", owner.getEffectiveName());
			}
		}

		repls.put("%now%", Constants.TIMESTAMP_R.formatted(System.currentTimeMillis() / 1000));

		for (Map.Entry<String, String> rep : repls.entrySet()) {
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

	public static CompletionStage<StashedCard> selectOption(I18N locale, GuildMessageChannel channel, Collection<StashedCard> cards, Card card, User user) {
		List<StashedCard> matches = cards.stream()
				.filter(sc -> sc.getCard().equals(card))
				.sorted(
						Comparator.comparing(StashedCard::getType)
								.thenComparing(StashedCard::getId, Comparator.reverseOrder())
								.thenComparing(sc -> sc.getCard().getId())
				).toList();

		if (matches.isEmpty()) return CompletableFuture.failedStage(new NoResultException());
		if (matches.size() == 1) return CompletableFuture.completedStage(matches.getFirst());

		AtomicInteger i = new AtomicInteger();
		return selectOption(locale, channel, matches, sc -> new StashItem(locale, sc).toString(i.getAndIncrement()), user);
	}

	public static <T> CompletionStage<T> selectOption(I18N locale, GuildMessageChannel channel, Collection<T> items, Function<T, String> generator, User user) {
		List<T> matches = List.copyOf(items);

		if (matches.isEmpty()) return CompletableFuture.failedStage(new NoResultException());
		if (matches.size() == 1) return CompletableFuture.completedStage(matches.getFirst());

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/choose_option"));

		List<Page> pages = generatePages(eb, matches, 10, 5, generator);

		Message msg = paginate(pages, channel, user);

		CompletableFuture<T> out = new CompletableFuture<>();
		awaitMessage(user, channel,
				m -> {
					if (!StringUtils.isNumeric(m.getContentRaw())) return false;

					try {
						int indx = Integer.parseInt(m.getContentRaw());
						out.complete(matches.get(indx));
					} catch (RuntimeException e) {
						out.complete(null);
					}

					msg.delete().queue(null, Utils::doNothing);
					return true;
				}, 1, TimeUnit.MINUTES, out
		);

		return out;
	}

	public static String didYouMean(String word, Collection<String> options) {
		String match = "";
		int threshold = Integer.MAX_VALUE;
		LevenshteinDistance checker = new LevenshteinDistance();

		for (String w : options) {
			if (word.equalsIgnoreCase(w)) {
				return w;
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

	public static String didYouMean(String word, @Language("PostgreSQL") String query, Object... params) {
		return DAO.queryNative(String.class, """
				SELECT x."value"
				FROM (
				     SELECT x."value", levenshtein_less_equal(cast(x."value" AS TEXT), cast('%1$s' AS TEXT), 5) AS dist
				     FROM (%2$s) x
				     ) x
				WHERE x.dist <= 5
				ORDER BY x.dist
				""".formatted(word, query), params);
	}

	public static Pair<CommandLine, Options> getCardCLI(I18N locale, String[] args, boolean market) {
		String[] longOp = {"name", "rarity", "anime", "chrome", "kawaipon", "senshi", "evogear", "field", "valid", "locked", "min", "max", "mine"};
		String[] shortOp = {"n", "r", "a", "c", "k", "s", "e", "f", "v", "l", "gt", "lt", "m"};

		Options opt = new Options();
		List<String> hasParam = List.of("n", "r", "a", "gt", "lt");
		for (int i = 0; i < longOp.length; i++) {
			if (market && i == 8) continue;
			else if (!market && i >= shortOp.length - 3) break;

			String lOp = longOp[i];
			String sOp = shortOp[i];
			opt.addOption(sOp, lOp, hasParam.contains(sOp), locale.get("search/" + lOp));
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
			return klass.cast(Utils.getOr(n, 0).shortValue());
		} else if (klass == Integer.class) {
			return klass.cast(Utils.getOr(n, 0).intValue());
		} else if (klass == Long.class) {
			return klass.cast(Utils.getOr(n, 0).longValue());
		} else if (klass == Float.class) {
			return klass.cast(Utils.getOr(n, 0).floatValue());
		} else if (klass == Double.class) {
			return klass.cast(Utils.getOr(n, 0).doubleValue());
		} else if (klass == Byte.class) {
			return klass.cast(Utils.getOr(n, 0).byteValue());
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
		return toShuffledList(new SplittableRandom());
	}

	public static <T> Collector<T, ?, List<T>> toShuffledList(RandomGenerator rng) {
		return Collectors.collectingAndThen(
				Collectors.toList(),
				l -> {
					Utils.shuffle(l, rng);
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
		if (out == null && wrap) out = sequence.getFirst();

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
		if (wrap) out = sequence.getLast();

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
		return exec(null, code, variables);
	}

	public static Object exec(String issuer, @Language("Groovy") String code) {
		return exec(issuer, code, Map.of());
	}

	public static Object exec(String issuer, @Language("Groovy") String code, Map<String, Object> variables) {
		Script script = compile(code);
		script.setBinding(new Binding(variables));

		if (issuer == null) {
			return script.run();
		} else {
			Instant start = Instant.now();
			try {
				return script.run();
			} finally {
				int runtime = Math.toIntExact(Duration.between(start, Instant.now()).toMillis());
				new ScriptMetrics(issuer, code, runtime).save();
			}
		}
	}

	public static Script compile(@Language("Groovy") String code) {
		Class<? extends Script> cached = Main.getCacheManager().computeScript(
				Calc.hash(code, "sha1"),
				(k, v) -> v == null ? Constants.GROOVY.load().parse(code).getClass() : v
		);

		try {
			return cached.getConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <K, V> void shufflePairs(Map<K, V> map) {
		shufflePairs(map, new SplittableRandom());
	}

	public static <K, V> void shufflePairs(Map<K, V> map, RandomGenerator rng) {
		List<V> valueList = new ArrayList<>(map.values());
		Utils.shuffle(valueList, rng);
		Iterator<V> valueIt = valueList.iterator();

		for (Map.Entry<K, V> e : map.entrySet()) {
			e.setValue(valueIt.next());
		}
	}

	public static String shorten(double number) {
		if (number < 1000) return roundToString(number, 1);

		NavigableMap<Double, String> suf = new TreeMap<>();
		suf.put(1_000D, "k");
		suf.put(1_000_000D, "m");
		suf.put(1_000_000_000D, "b");
		suf.put(1_000_000_000_000D, "t");
		suf.put(1_000_000_000_000_000D, "q");

		Map.Entry<Double, String> entry = suf.floorEntry(number);
		return roundToString(number / entry.getKey(), 1) + entry.getValue();
	}

	public static String replaceTags(String text, char delimiter, Map<String, Object> tags) {
		for (Map.Entry<String, Object> tag : tags.entrySet()) {
			text = text.replace(delimiter + tag.getKey() + delimiter, String.valueOf(tag.getValue()));
		}

		return text;
	}

	public static Webhook getWebhook(StandardGuildMessageChannel channel) {
		if (!channel.getGuild().getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) return null;

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

	public static <T> T with(T t, Consumer<T> act) {
		act.accept(t);
		return t;
	}

	public static <T, R> R with(T t, Function<T, R> act) {
		return act.apply(t);
	}

	public static <A, T extends Collection<A>> List<A> padList(T col, int pad) {
		List<A> out = new ArrayList<>(col);
		for (int i = 0; i < pad - col.size(); i++) {
			out.add(null);
		}

		return out;
	}

	public static RichCustomEmoji getEmote(String name) {
		String[] repos = {
				Constants.EMOTE_REPO_1,
				Constants.EMOTE_REPO_2,
				Constants.EMOTE_REPO_3,
				Constants.EMOTE_REPO_4,
				Constants.EMOTE_REPO_5,
		};

		for (String repo : repos) {
			Guild g = Main.getApp().getShiro().getGuildById(repo);
			if (g == null) continue;

			List<RichCustomEmoji> emotes = g.getEmojisByName(name, true);
			if (emotes.isEmpty()) continue;

			return emotes.getFirst();
		}

		return null;
	}

	public static String getEmoteString(String name) {
		RichCustomEmoji e = getEmote(name);
		return e == null ? "" : e.getAsMention();
	}

	public static int getDigits(long n) {
		if (n < 1e10) {
			if (n < 1e5) {
				if (n < 1e3) {
					if (n < 1e2) {
						return n < 1e1 ? 1 : 2;
					}

					return 3;
				} else {
					if (n < 1e4) {
						return 4;
					}

					return 5;
				}
			} else {
				if (n < 1e7) {
					if (n < 1e6) {
						return 6;
					}

					return 7;
				} else {
					if (n < 1e9) {
						if (n < 1e8) {
							return 8;
						}

						return 9;
					}

					return 10;
				}
			}
		} else {
			if (n < 1e14) {
				if (n < 1e12) {
					if (n < 1e11) {
						return 11;
					}

					return 12;
				} else {
					if (n < 1e13) {
						return 13;
					}

					return 14;
				}
			} else {
				if (n < 1e16) {
					if (n < 1e15) {
						return 15;
					}

					return 16;
				} else {
					if (n < 1e17) {
						return 17;
					}

					return n < 1e18 ? 18 : 19;
				}
			}
		}
	}

	public static <T, C extends Collection<T>> List<T> flatten(Collection<C> col) {
		return col.stream().flatMap(C::stream).collect(Collectors.toList());
	}

	public static <T> T luckyRoll(Supplier<T> supplier, BiFunction<T, T, Boolean> comparator) {
		T out = null;
		for (int i = 0; i < 2; i++) {
			T roll = supplier.get();
			if (out == null || comparator.apply(out, roll)) {
				out = roll;
			}
		}

		return out;
	}

	public static <T> void shuffle(List<T> col) {
		shuffle(col, Constants.DEFAULT_RNG.get());
	}

	@SuppressWarnings("unchecked")
	public static <T> void shuffle(List<T> list, RandomGenerator rng) {
		int size = list.size();
		if (size < 5 || list instanceof RandomAccess) {
			for (int i = size; i > 1; i--) {
				Collections.swap(list, i - 1, rng.nextInt(i));
			}
		} else {
			Object[] arr = list.toArray();

			for (int i = size; i > 1; i--) {
				ArrayUtils.swap(arr, i - 1, rng.nextInt(i));
			}

			ListIterator<T> it = list.listIterator();
			for (Object e : arr) {
				it.next();
				it.set((T) e);
			}
		}
	}

	public static MessageCreateAction sendPage(MessageChannel channel, Page p) {
		if (p.getContent() instanceof String s) {
			return channel.sendMessage(s);
		} else if (p.getContent() instanceof MessageEmbed e) {
			return channel.sendMessageEmbeds(e);
		} else if (p.getContent() instanceof EmbedCluster c) {
			return channel.sendMessageEmbeds(c.getEmbeds());
		}

		throw new IllegalArgumentException();
	}

	public static List<MessageEmbed> getEmbeds(Page p) {
		if (p.getContent() instanceof MessageEmbed e) {
			return List.of(e);
		} else if (p.getContent() instanceof EmbedCluster c) {
			return c.getEmbeds();
		}

		return List.of();
	}

	public static <T> T map(Class<T> target, Object... args) {
		for (Constructor<?> ctr : target.getConstructors()) {
			if (ctr.getParameterCount() != args.length) continue;

			try {
				AtomicInteger i = new AtomicInteger();
				Class<?>[] types = ctr.getParameterTypes();
				return target.cast(ctr.newInstance(
						Arrays.stream(args)
								.map(o -> {
									int idx = i.getAndIncrement();

									if (o instanceof Number n) {
										if (n.doubleValue() - n.intValue() != 0) {
											return n.floatValue();
										}

										return n.intValue();
									} else if (o instanceof Boolean b) {
										return b;
									}

									return types[idx].cast(o);
								})
								.toArray()
				));
			} catch (InvocationTargetException | InstantiationException | IllegalAccessException ignore) {
			}
		}

		return null;
	}

	public static int digits(double number) {
		return BigDecimal.valueOf(number - (int) number).precision();
	}
}
