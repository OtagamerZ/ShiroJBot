/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.WebhookCluster;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.ColorlessWebhookEmbedBuilder;
import com.kuuhaku.model.common.Extensions;
import com.kuuhaku.model.common.GifFrame;
import com.kuuhaku.model.common.drop.*;
import com.kuuhaku.model.enums.*;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.model.persistent.guild.Buff;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.buttons.Button;
import com.kuuhaku.model.persistent.guild.buttons.ButtonChannel;
import com.kuuhaku.model.persistent.guild.buttons.ButtonMessage;
import com.squareup.moshi.JsonDataException;
import de.androidpit.colorthief.ColorThief;
import io.github.furstenheim.CopyDown;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.gif.DisposalMethod;
import org.apache.commons.imaging.formats.gif.GifImageMetadata;
import org.apache.commons.imaging.formats.gif.GifImageMetadataItem;
import org.apache.commons.imaging.formats.gif.GifImageParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Precision;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static net.dv8tion.jda.api.Permission.*;

public abstract class Helper {
	public static final String VOID = "\u200B";
	public static final String CANCEL = "❎";
	public static final String ACCEPT = "✅";
	public static final String ANTICOPY = "\uFFF8"; //or U+034F
	public static final String MENTION = "<@\\d+>|<@!\\d+>";
	public static final String TIMESTAMP = "<t:%s:R>";
	public static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
	public static final int CANVAS_SIZE = 2049;
	public static final DateTimeFormatter FULL_DATE_FORMAT = DateTimeFormatter.ofPattern(I18n.getString("full-date-format"));
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(I18n.getString("date-format"));
	public static final CopyDown htmlConverter = new CopyDown();
	public static final String HOME = "674261700366827539";
	public static final Random DEFAULT_RNG = new Random();
	public static final Random DEFAULT_SECURE_RNG = new SecureRandom();
	public static final int BASE_CARD_PRICE = 300;
	public static final int BASE_EQUIPMENT_PRICE = 2250;
	public static final int BASE_FIELD_PRICE = 35000;
	public static final long MILLIS_IN_DAY = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
	public static final long MILLIS_IN_HOUR = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
	public static final long MILLIS_IN_MINUTE = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
	public static final long MILLIS_IN_SECOND = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
	public static final long ALL_MUTE_PERMISSIONS = Permission.getRaw(
			MESSAGE_ADD_REACTION, MESSAGE_WRITE, MESSAGE_TTS,
			MESSAGE_MANAGE, MESSAGE_EMBED_LINKS, MESSAGE_ATTACH_FILES,
			MESSAGE_MENTION_EVERYONE, USE_SLASH_COMMANDS
	);

	private static PrivilegeLevel getPrivilegeLevel(Member member) {
		if (member == null)
			return PrivilegeLevel.USER;
		else if (ShiroInfo.getNiiChan().equals(member.getId()))
			return PrivilegeLevel.NIICHAN;
		else if (StaffDAO.getUser(member.getId()).getType().isAllowed(StaffType.DEVELOPER))
			return PrivilegeLevel.DEV;
		else if (StaffDAO.getUser(member.getId()).getType().isAllowed(StaffType.SUPPORT))
			return PrivilegeLevel.SUPPORT;
		else if (member.hasPermission(Permission.MESSAGE_MANAGE))
			return PrivilegeLevel.MOD;
		else if (TagDAO.getTagById(member.getGuild().getOwnerId()).isBeta() || TagDAO.getTagById(member.getId()).isBeta())
			return PrivilegeLevel.BETA;
		return PrivilegeLevel.USER;
	}

	public static boolean hasPermission(Member member, PrivilegeLevel privilegeLevel) {
		return getPrivilegeLevel(member).hasAuthority(privilegeLevel);
	}

	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		return Precision.round(value, places);
	}

	public static String roundToString(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		return new DecimalFormat("#,##0" + (places > 0 ? "." : "") + StringUtils.repeat("#", places)).format(value);
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

	public static boolean findURL(String text) {
		Map<String, String> leetSpeak = Map.of(
				"(1|!)", "i",
				"3", "e",
				"4", "a",
				"5", "s",
				"7", "t",
				"0", "o",
				"(@|#|$|%|&|*)", "."
		);

		final Pattern urlPattern = Pattern.compile(
				"(((ht|f)tp|ws)(s?)://|www\\.)([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*[\\w.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*?",
				Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
		text = StringUtils.deleteWhitespace(text);
		text = replaceWith(text, leetSpeak);

		final Matcher msg = urlPattern.matcher(text.toLowerCase(Locale.ROOT));
		return msg.find() && Extensions.checkExtension(text);
	}

	public static boolean isUrl(String text) {
		return new UrlValidator().isValid(text);
	}

	public static boolean findMentions(String text) {
		final Pattern everyone = Message.MentionType.EVERYONE.getPattern();
		final Pattern here = Message.MentionType.HERE.getPattern();

		return everyone.matcher(text).matches() || here.matcher(text).matches();
	}

	public static void sendPM(User user, String message) {
		user.openPrivateChannel().queue((channel) -> channel.sendMessage(message).queue(null, Helper::doNothing));
	}

	public static void typeMessage(MessageChannel channel, String message) {
		channel.sendTyping()
				.delay(message.length() * 25 > 10000 ? 10000 : message.length() + 500, TimeUnit.MILLISECONDS)
				.flatMap(s -> channel.sendMessage(makeEmoteFromMention(message)))
				.queue(null, Helper::doNothing);
	}

	public static void typeMessage(MessageChannel channel, String message, Message target) {
		channel.sendTyping()
				.delay(message.length() * 25 > 10000 ? 10000 : message.length() + 500, TimeUnit.MILLISECONDS)
				.flatMap(s -> target.reply(makeEmoteFromMention(message)))
				.queue(null, Helper::doNothing);
	}

	public static int rng(int max) {
		return rng(0, max, DEFAULT_RNG);
	}

	public static int rng(int max, long seed) {
		return rng(0, max, new Random(seed));
	}

	public static int rng(int max, Random random) {
		return rng(0, max, random);
	}

	public static int rng(int min, int max) {
		return rng(min, max, DEFAULT_RNG);
	}

	public static int rng(int min, int max, long seed) {
		return rng(min, max, new Random(seed));
	}

	public static int rng(int min, int max, Random random) {
		return (int) Math.round(min + random.nextDouble() * (max - min));
	}

	public static double rng(double max) {
		return rng(0, max, DEFAULT_RNG);
	}

	public static double rng(double max, long seed) {
		return rng(0, max, new Random(seed));
	}

	public static double rng(double max, Random random) {
		return rng(0, max, random);
	}

	public static double rng(double min, double max) {
		return rng(min, max, DEFAULT_RNG);
	}

	public static double rng(double min, double max, long seed) {
		return rng(min, max, new Random(seed));
	}

	public static double rng(double min, double max, Random random) {
		return min + random.nextDouble() * (max - min);
	}

	public static Color colorThief(String url) throws IOException {
		BufferedImage icon = ImageIO.read(getImage(url));

		return colorThief(icon);
	}

	public static Color colorThief(BufferedImage image) {
		try {
			if (image != null) {
				int[] colors = ColorThief.getColor(image, 5, false);
				return new Color(colors[0], colors[1], colors[2]);
			} else return getRandomColor();
		} catch (NullPointerException e) {
			return getRandomColor();
		}
	}

	public static void spawnAd(Account acc, MessageChannel channel) {
		if (!acc.hasVoted(false) && chance(1)) {
			channel.sendMessage("Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\nhttps://top.gg/bot/572413282653306901").queue();
		}
	}

	public static Logger logger(Class<?> source) {
		return LogManager.getLogger(source.getName());
	}

	public static InputStream getImage(String link) throws IOException {
		return new URL(link).openStream();
	}

	public static Webhook getOrCreateWebhook(TextChannel chn, String name, JDA bot) throws InterruptedException, ExecutionException {
		final Webhook[] webhook = {null};
		List<Webhook> whs = chn.retrieveWebhooks().submit().get();
		whs.stream()
				.filter(w -> Objects.requireNonNull(w.getOwner()).getUser() == bot.getSelfUser())
				.findFirst()
				.ifPresent(w -> webhook[0] = w);

		try {
			if (webhook[0] == null) return Pages.subGet(chn.createWebhook(name));
			else {
				webhook[0].getUrl();
				return webhook[0];
			}
		} catch (NullPointerException e) {
			return Pages.subGet(chn.createWebhook(name));
		}
	}

	public static Webhook getOrCreateWebhook(TextChannel chn, String name) throws InterruptedException, ExecutionException {
		AtomicReference<Webhook> webhook = new AtomicReference<>();
		List<Webhook> whs = chn.retrieveWebhooks().submit().get();
		whs.stream()
				.filter(w -> {
					Member m = w.getOwner();
					return m != null && m.equals(chn.getGuild().getSelfMember());
				})
				.findFirst()
				.ifPresent(webhook::set);

		try {
			if (webhook.get() == null)
				return Pages.subGet(chn.createWebhook(name));
			else {
				return webhook.get();
			}
		} catch (NullPointerException e) {
			return Pages.subGet(chn.createWebhook(name));
		}
	}

	public static Color reverseColor(Color c) {
		float[] hsv = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsv);
		hsv[0] = (hsv[0] * 360 + 180) / 360;

		return Color.getHSBColor(hsv[0], hsv[1], hsv[2]);
	}

	public static String unmention(String text) {
		return text.replace("@everyone", bugText("@everyone")).replace("@here", bugText("@here"));
	}

	public static String makeEmoteFromMention(String text) {
		return unmention(text.replaceAll("\\{(a)?&(\\w+)&(\\d+)}", "<$1:$2:$3>"));
	}

	public static String makeTagFromEmote(String text) {
		return unmention(text.replaceAll("<a?(:\\w+:)\\d+>", "$1"));
	}

	public static void logToChannel(User u, boolean isCommand, PreparedCommand c, String msg, Guild g) {
		GuildConfig gc = GuildDAO.getGuildById(g.getId());
		if (gc == null || gc.getLogChannel() == null) return;

		TextChannel tc = gc.getLogChannel();
		if (tc == null) {
			gc.setLogChannel(null);
			return;
		}

		try {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(StringUtils.abbreviate(msg, 2048));
			if (u != null) eb.addField("Referente:", u.getAsMention(), true);
			if (isCommand) eb.addField("Comando:", gc.getPrefix() + c.getName(), true);
			eb.setTimestamp(Instant.now());

			tc.sendMessageEmbeds(eb.build()).queue(null, Helper::doNothing);
		} catch (Exception e) {
			gc.setLogChannel("");
			GuildDAO.updateGuildSettings(gc);
			logger(Helper.class).warn(e + " | " + e.getStackTrace()[0]);
			Member owner = g.getOwner();
			if (owner != null)
				owner.getUser().openPrivateChannel()
						.flatMap(ch -> ch.sendMessage("Canal de log invalidado com o seguinte erro: `%s | %s`".formatted(e.getClass().getSimpleName(), e)))
						.queue(null, Helper::doNothing);
		}
	}

	public static void logToChannel(User u, boolean isCommand, PreparedCommand c, String msg, Guild g, String args) {
		GuildConfig gc = GuildDAO.getGuildById(g.getId());
		if (gc.getLogChannel() == null) return;

		TextChannel tc = gc.getLogChannel();
		if (tc == null) {
			gc.setLogChannel(null);
			return;
		}

		try {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(StringUtils.abbreviate(msg, 2048));
			eb.addField("Referente:", u.getAsMention(), true);
			if (isCommand) {
				eb.addField("Comando:", gc.getPrefix() + c.getName(), true);
				eb.addField("Argumentos:", StringUtils.abbreviate(args, 1024), true);
			}
			eb.setTimestamp(Instant.now());

			tc.sendMessageEmbeds(eb.build()).queue(null, Helper::doNothing);
		} catch (Exception e) {
			gc.setLogChannel("");
			GuildDAO.updateGuildSettings(gc);
			logger(Helper.class).warn(e + " | " + e.getStackTrace()[0]);
			Member owner = g.getOwner();
			if (owner != null)
				owner.getUser().openPrivateChannel()
						.flatMap(ch -> ch.sendMessage("Canal de log invalidado com o seguinte erro: `%s | %s`".formatted(e.getClass().getSimpleName(), e)))
						.queue(null, Helper::doNothing);
		}
	}

	public static String getRandomHexColor() {
		String[] colorTable = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			sb.append(colorTable[clamp(rng(16), 0, 16)]);
		}
		return "#" + sb;
	}

	public static Color textToColor(String text) {
		CRC32 crc = new CRC32();
		crc.update(text.getBytes(StandardCharsets.UTF_8));
		return Color.decode("#%06x".formatted(crc.getValue() & 0xFFFFFF));
	}

	public static Color getRandomColor() {
		return Color.decode("#%06x".formatted(rng(0xFFFFFF)));
	}

	public static Color getRandomColor(long seed) {
		return Color.decode("#%06x".formatted(rng(0xFFFFFF, seed)));
	}

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
	public static <T> boolean equalsAll(T value, T... compareWith) {
		return Arrays.stream(compareWith).allMatch(value::equals);
	}

	public static boolean equalsAny(String string, String... compareWith) {
		return Arrays.stream(compareWith).anyMatch(string::equalsIgnoreCase);
	}

	@SafeVarargs
	public static <T> boolean equalsAny(T value, T... compareWith) {
		return Arrays.asList(compareWith).contains(value);
	}

	public static boolean hasPermission(Member m, Permission p, TextChannel c) {
		return m.getPermissions(c).contains(p);
	}

	public static String getCurrentPerms(TextChannel c) {
		Member shiro = c.getGuild().getSelfMember();
		EnumSet<Permission> perms = shiro.getPermissionsExplicit(c);

		return "__**Permissões atuais da Shiro**__\n\n" +
				perms.stream()
						.map(p -> "✅ -> " + p.getName())
						.sorted()
						.collect(Collectors.joining("\n"));
	}

	@SuppressWarnings("unchecked")
	public static <T> T getOr(Object get, T or) {
		if (get instanceof String s && s.isBlank()) return or;
		else return get == null ? or : (T) get;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getOrMany(Object get, T... or) {
		T out = null;

		for (T t : or) {
			out = getOr(get, t);
			if (out != null && !(out instanceof String s && s.isBlank())) break;
		}

		return out;
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

	public static void nonBetaAlert(User author, Member member, MessageChannel channel, String s, String link) {
		try {
			if (!TagDAO.getTagById(author.getId()).isBeta() && !hasPermission(member, PrivilegeLevel.DEV)) {
				channel.sendMessage("❌ | Este comando requer acesso beta!").queue();
				return;
			}
		} catch (NoResultException e) {
			channel.sendMessage("❌ | Este comando requer acesso beta!").queue();
			return;
		}

		channel.sendMessage("Link enviado no privado!").queue();

		EmbedBuilder eb = new EmbedBuilder();

		eb.setThumbnail("https://img.icons8.com/cotton/2x/checkmark.png");
		eb.setTitle("Olá, obrigada por apoiar meu desenvolvimento!");
		eb.setDescription(s + System.getenv(link));
		eb.setColor(Color.green);

		author.openPrivateChannel().queue(c -> c.sendMessageEmbeds(eb.build()).queue());
	}

	public static void finishEmbed(Guild guild, List<Page> pages, List<MessageEmbed.Field> f, EmbedBuilder eb, int i) {
		eb.setColor(getRandomColor());
		eb.setAuthor("Para usar estes emotes, utilize o comando \"" + GuildDAO.getGuildById(guild.getId()).getPrefix() + "say MENÇÃO\"");
		eb.setFooter("Página " + (i + 1) + ". Mostrando " + (-10 + 10 * (i + 1)) + " - " + (Math.min(10 * (i + 1), f.size())) + " resultados.", null);

		pages.add(new InteractPage(eb.build()));
	}

	public static void refreshButtons(GuildConfig gc) {
		Set<ButtonChannel> channels = gc.getButtonConfigs();
		if (channels.isEmpty()) return;

		Guild g = Main.getInfo().getGuildByID(gc.getGuildId());
		if (g != null) {
			for (ButtonChannel channel : channels) {
				TextChannel chn = g.getTextChannelById(channel.getId());

				if (chn == null) {
					gc.getButtonConfigs().remove(channel);
					GuildDAO.updateGuildSettings(gc);
				} else {
					for (ButtonMessage message : channel.getMessages()) {
						Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
						Message msg;
						try {
							msg = chn.retrieveMessageById(message.getId()).submit().get();
						} catch (MissingAccessException | ExecutionException | InterruptedException e) {
							GuildConfig conf = GuildDAO.getGuildById(g.getId());
							for (ButtonChannel bc : conf.getButtonConfigs()) {
								if (bc.removeMessage(message)) break;
							}

							GuildDAO.updateGuildSettings(gc);
							continue;
						}
						resolveButton(g, message.getButtons(), buttons);

						if (Helper.hasPermission(g.getSelfMember(), MESSAGE_MANAGE, chn))
							msg.clearReactions().queue();

						if (message.isGatekeeper()) {
							Role r = message.getRole(g);

							gatekeep(msg, r);
						} else {
							buttons.put(Helper.parseEmoji(CANCEL), wrapper -> {
								if (wrapper.getUser().getId().equals(message.getAuthor())) {
									GuildConfig conf = GuildDAO.getGuildById(g.getId());
									for (ButtonChannel bc : conf.getButtonConfigs()) {
										if (bc.removeMessage(message)) break;
									}

									GuildDAO.updateGuildSettings(conf);
									wrapper.getMessage().clearReactions().queue();
								}
							});

							Pages.buttonize(msg, buttons, ShiroInfo.USE_BUTTONS, true);
						}
					}
				}
			}
		}
	}

	public static void resolveButton(Guild g, List<Button> jo, Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons) {
		for (Button b : jo) {
			Role role = b.getRole(g);

			buttons.put(Helper.parseEmoji(b.getEmote()), wrapper -> {
				if (role != null) {
					try {
						Member m = wrapper.getMember();
						if (m.getRoles().contains(role)) {
							g.removeRoleFromMember(m, role).queue(null, Helper::doNothing);
						} else {
							g.addRoleToMember(wrapper.getMember(), role).queue(null, Helper::doNothing);
						}
					} catch (InsufficientPermissionException | HierarchyException ignore) {
					}
				} else {
					wrapper.getMessage().clearReactions().queue(s -> {
						wrapper.getChannel().sendMessage(":warning: | Botão removido devido a cargo inexistente.").queue();
						GuildConfig gc = GuildDAO.getGuildById(g.getId());

						b.getParent().removeButton(b);

						GuildDAO.updateGuildSettings(gc);
					});
				}
			});
		}
	}

	public static void gatekeep(Message m, Role r) {
		if (m == null) return;

		Pages.buttonize(m, new LinkedHashMap<>() {{
			put(parseEmoji("☑"), wrapper -> {
				try {
					wrapper.getMessage().getGuild().addRoleToMember(wrapper.getMember(), r).queue();
				} catch (InsufficientPermissionException | HierarchyException ignore) {
				}
			});
			put(parseEmoji("\uD83D\uDEAA"), wrapper -> {
				try {
					wrapper.getMember().kick("Não aceitou as regras.").queue();
				} catch (InsufficientPermissionException | HierarchyException ignore) {
				}
			});
		}}, ShiroInfo.USE_BUTTONS, false);
	}

	public static void addButton(String[] args, Message message, MessageChannel channel, GuildConfig gc, String s2, boolean gatekeeper) {
		Role r = message.getMentionedRoles().get(0);

		Set<ButtonChannel> channels = gc.getButtonConfigs();
		ButtonChannel bc = channels.stream()
				.filter(chn -> chn.getId().equals(channel.getId()))
				.findFirst()
				.orElse(null);

		if (bc == null) {
			bc = new ButtonChannel(channel.getId());
			gc.getButtonConfigs().add(bc);
		}

		Set<ButtonMessage> messages = bc.getMessages();
		ButtonMessage bm = messages.stream()
				.filter(msg -> msg.getId().equals(args[0]))
				.findFirst()
				.orElse(null);

		if (bm == null) {
			bm = new ButtonMessage(
					args[0],
					message.getAuthor().getId(),
					gatekeeper,
					gatekeeper ? r.getId() : null
			);

			if (gatekeeper && bc.getMessages().stream().anyMatch(ButtonMessage::isGatekeeper)) {
				Set<ButtonMessage> msgs = Set.copyOf(bc.getMessages());
				for (ButtonMessage bMsg : msgs) {
					if (bMsg.isGatekeeper()) {
						bc.removeMessage(bMsg);
					}
				}
			}

			bc.addMessage(bm);
		}

		if (!gatekeeper) {
			String id;
			if (parseEmoji(s2).isUnicode()) {
				id = s2;
			} else {
				Emote e = Main.getShiroShards().getEmoteById(s2);
				if (e == null) throw new IllegalArgumentException();
				else id = e.getId();
			}

			bm.addButton(new Button(r.getId(), id));
		}

		GuildDAO.updateGuildSettings(gc);
	}

	public static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
		int original_width = imgSize.width;
		int original_height = imgSize.height;
		int bound_width = boundary.width;
		int bound_height = boundary.height;
		int new_width = original_width;
		int new_height = original_height;

		if (original_width > bound_width) {
			new_width = bound_width;
			new_height = (new_width * original_height) / original_width;
		}

		if (new_height > bound_height) {
			new_height = bound_height;
			new_width = (new_height * original_width) / original_height;
		}

		return new Dimension(new_width, new_height);
	}

	public static InviteAction createInvite(Guild guild) {
		TextChannel def = guild.getDefaultChannel();
		if (def != null && guild.getSelfMember().hasPermission(def, Permission.CREATE_INSTANT_INVITE)) {
			return def.createInvite();
		}

		for (TextChannel tc : guild.getTextChannels()) {
			if (guild.getSelfMember().hasPermission(tc, Permission.CREATE_INSTANT_INVITE))
				return tc.createInvite();
		}

		return null;
	}

	public static String didYouMean(String word, String[] array) {
		String match = "";
		int threshold = 999;
		LevenshteinDistance checker = new LevenshteinDistance();

		for (String w : array) {
			if (word.equalsIgnoreCase(w)) {
				return word;
			} else {
				int diff = checker.apply(word.toLowerCase(Locale.ROOT), w.toLowerCase(Locale.ROOT));
				if (diff < threshold) {
					match = w;
					threshold = diff;
				}
			}
		}

		return match;
	}

	public static String replaceEmotes(String msg) {
		String[] args = msg.split(" ");

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(":") && args[i].endsWith(":")) {
				Emote e = Main.getShiroShards().getEmoteById(ShiroInfo.getEmoteLookup().get(args[i]));
				if (e != null)
					args[i] = e.getAsMention();
			}
		}

		return String.join(" ", args);
	}

	public static ByteArrayOutputStream renderMeme(String text, BufferedImage bi) throws IOException {
		String[] lines = text.split("\\r?\\n");
		List<String> wrappedLines = new ArrayList<>();

		BufferedImage canvas = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D g2d = canvas.createGraphics();
		g2d.setFont(new Font("Arial", Font.BOLD, 30));

		StringBuilder sb = new StringBuilder();

		for (String line : lines) {
			sb.setLength(0);
			for (String word : line.split(" ")) {
				if (g2d.getFontMetrics().stringWidth(sb + word) > bi.getWidth() - 50) {
					wrappedLines.add(sb.toString().trim());
					sb.setLength(0);
				}
				sb.append(word).append(" ");
			}
			if (sb.length() > 0) wrappedLines.add(sb.toString());
		}
		if (wrappedLines.isEmpty()) wrappedLines.add(text);

		canvas = new BufferedImage(bi.getWidth(), 45 + (45 * wrappedLines.size()) + bi.getHeight(), BufferedImage.TYPE_INT_RGB);
		g2d = canvas.createGraphics();

		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("Arial", Font.BOLD, 30));
		for (int i = 0; i < wrappedLines.size(); i++) g2d.drawString(wrappedLines.get(i), 25, 45 + (45 * i));
		g2d.drawImage(bi, 0, canvas.getHeight() - bi.getHeight(), null);

		g2d.dispose();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(canvas, "png", baos);
		return baos;
	}

	public static String sendEmotifiedString(Guild g, String text) {
		for (Emote e : g.getEmotes()) {
			if (e.getName().startsWith("TEMP_")) {
				try {
					e.delete().queue(null, Helper::doNothing);
				} catch (ErrorResponseException ignore) {
				}
			}
		}

		text = makeEmoteFromMention(text);
		text = makeTagFromEmote(text);

		String[] lines = text.split("\n");
		for (int l = 0; l < lines.length; l++) {
			String[] words = lines[l].split(" ");
			for (int i = 0, emotes = 0, slots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(e -> !e.isAnimated()).count(), aSlots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(Emote::isAnimated).count(); i < words.length && emotes < 10; i++) {
				String word = words[i];
				if (!word.matches(":.+:")) {
					words[i] = word;
					continue;
				}

				String id = ShiroInfo.getEmoteLookup().get(word);
				Emote e = id == null ? null : Main.getShiroShards().getEmoteById(id);

				if (e != null) {
					try {
						boolean animated = e.isAnimated();
						if ((animated ? aSlots : slots) > 0) {
							e = Pages.subGet(g.createEmote(
									"TEMP_" + e.getName(),
									Icon.from(getImage(e.getImageUrl())),
									g.getSelfMember().getRoles().get(0)
							));

							if (animated) aSlots--;
							else slots--;
						}

						words[i] = e.getAsMention();
						emotes++;
					} catch (IOException ex) {
						logger(Helper.class).error(ex + " | " + ex.getStackTrace()[0]);
					}
				}
			}

			lines[l] = String.join(" ", words);
		}

		return String.join("\n", lines);
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

	public static void doNothing(Throwable t) {
		try {
			logger(Helper.class).debug(t + " | " + t.getStackTrace()[0]);
			throw t;
		} catch (Throwable ignore) {
		}
	}

	public static float offsetPrcnt(float value, float max, float offset) {
		return (value - offset) / (max - offset);
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

	public static int prcntToInt(float value, float max) {
		return prcntToInt(value, max, RoundingMode.UNNECESSARY);
	}

	public static int prcntToInt(float value, float max, RoundingMode mode) {
		return (int) switch (mode) {
			case UP, CEILING, HALF_UP -> Math.ceil((value * 100) / max);
			case DOWN, FLOOR, HALF_DOWN -> Math.floor((value * 100) / max);
			case HALF_EVEN, UNNECESSARY -> Math.round((value * 100) / max);
		};
	}

	public static JSONObject post(String endpoint, JSONObject payload, String token) {
		try {
			HttpPost req = new HttpPost(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			req.setEntity(new StringEntity(payload.toString()));

			URI uri = ub.build();

			req.setHeaders(new Header[]{
					new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8"),
					new BasicHeader(HttpHeaders.AUTHORIZATION, token)
			});
			req.setURI(uri);

			try (CloseableHttpResponse res = ShiroInfo.getHttp().execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject post(String endpoint, JSONObject payload, Map<String, String> headers) {
		try {
			HttpPost req = new HttpPost(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			req.setEntity(new StringEntity(payload.toString()));

			URI uri = ub.build();

			req.setHeaders(headers.entrySet().parallelStream()
					.map(e -> new BasicHeader(e.getKey(), e.getValue()))
					.toArray(Header[]::new)
			);
			req.setURI(uri);

			try (CloseableHttpResponse res = ShiroInfo.getHttp().execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject post(String endpoint, JSONObject payload, Map<String, String> headers, String token) {
		try {
			HttpPost req = new HttpPost(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			req.setEntity(new StringEntity(payload.toString()));

			URI uri = ub.build();

			req.setHeaders(headers.entrySet().parallelStream()
					.map(e -> new BasicHeader(e.getKey(), e.getValue()))
					.toArray(Header[]::new)
			);
			req.setHeader(HttpHeaders.AUTHORIZATION, token);
			req.setURI(uri);

			try (CloseableHttpResponse res = ShiroInfo.getHttp().execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject post(String endpoint, String payload, Map<String, String> headers, String token) {
		try {
			HttpPost req = new HttpPost(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			req.setEntity(new StringEntity(payload));

			URI uri = ub.build();

			req.setHeaders(headers.entrySet().parallelStream()
					.map(e -> new BasicHeader(e.getKey(), e.getValue()))
					.toArray(Header[]::new)
			);
			req.setHeader(HttpHeaders.AUTHORIZATION, token);
			req.setURI(uri);

			try (CloseableHttpResponse res = ShiroInfo.getHttp().execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject get(String endpoint, JSONObject payload) {
		try {
			HttpGet req = new HttpGet(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			for (Map.Entry<String, Object> params : payload.entrySet()) {
				ub.setParameter(params.getKey(), String.valueOf(params.getValue()));
			}

			URI uri = ub.build();

			req.setHeaders(new Header[]{
					new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8")
			});
			req.setURI(uri);

			try (CloseableHttpResponse res = ShiroInfo.getHttp().execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject get(String endpoint, JSONObject payload, String token) {
		try {
			HttpGet req = new HttpGet(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			for (Map.Entry<String, Object> params : payload.entrySet()) {
				ub.setParameter(params.getKey(), String.valueOf(params.getValue()));
			}

			URI uri = ub.build();

			req.setHeaders(new Header[]{
					new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8"),
					new BasicHeader(HttpHeaders.AUTHORIZATION, token)
			});
			req.setURI(uri);

			try (CloseableHttpResponse res = ShiroInfo.getHttp().execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject get(String endpoint, JSONObject payload, Map<String, String> headers, String token) {
		try {
			HttpGet req = new HttpGet(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			for (Map.Entry<String, Object> params : payload.entrySet()) {
				ub.setParameter(params.getKey(), String.valueOf(params.getValue()));
			}

			URI uri = ub.build();

			req.setHeaders(headers.entrySet().parallelStream()
					.map(e -> new BasicHeader(e.getKey(), e.getValue()))
					.toArray(Header[]::new)
			);
			req.addHeader(HttpHeaders.AUTHORIZATION, token);
			req.setURI(uri);

			try (CloseableHttpResponse res = ShiroInfo.getHttp().execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static String urlEncode(JSONObject payload) {
		String[] params = payload.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.toArray(String[]::new);

		return String.join("&", params);
	}

	public static String generateToken(String seed, int length) {
		byte[] nameSpace = seed.getBytes(StandardCharsets.UTF_8);
		byte[] randomSpace = new byte[length];
		Helper.DEFAULT_SECURE_RNG.nextBytes(randomSpace);

		return atob(nameSpace) + "." + atob(randomSpace);
	}

	public static void awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act) {
		ShiroInfo.getShiroEvents().addHandler(chn.getGuild(), new SimpleMessageListener(chn) {
			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getAuthor().getId().equals(u.getId())) {
					if (act.apply(event.getMessage())) close();
				}
			}
		});
	}

	public static void awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act, int time, TimeUnit unit) {
		ShiroInfo.getShiroEvents().addHandler(chn.getGuild(), new SimpleMessageListener(chn) {
			final ScheduledFuture<?> timeout = Executors.newSingleThreadScheduledExecutor().schedule(this::close, time, unit);

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getAuthor().getId().equals(u.getId())) {
					if (act.apply(event.getMessage())) {
						timeout.cancel(true);
						close();
					}
				}
			}
		});
	}

	public static void awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act, int time, TimeUnit unit, Runnable onTimeout) {
		ShiroInfo.getShiroEvents().addHandler(chn.getGuild(), new SimpleMessageListener(chn) {
			final ScheduledFuture<?> timeout = Executors.newSingleThreadScheduledExecutor().schedule(() -> {
				close();
				onTimeout.run();
			}, time, unit);

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getAuthor().getId().equals(u.getId())) {
					if (act.apply(event.getMessage())) {
						timeout.cancel(true);
						close();
					}
				}
			}
		});
	}

	public static <T> T getRandomEntry(Collection<T> col) {
		if (col.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");
		List<T> list = List.copyOf(col);

		return list.get(rng(list.size() - 1));
	}

	@SafeVarargs
	public static <T> T getRandomEntry(T... array) {
		if (array.length == 0) throw new IllegalArgumentException("Array must not be empty");
		List<T> list = List.of(array);

		return list.get(rng(list.size() - 1));
	}

	public static <T> T getRandomEntry(Random random, Collection<T> col) {
		if (col.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");
		List<T> list = List.copyOf(col);

		return list.get(rng(list.size() - 1, random));
	}

	@SafeVarargs
	public static <T> T getRandomEntry(Random random, T... array) {
		if (array.length == 0) throw new IllegalArgumentException("Array must not be empty");
		List<T> list = List.of(array);

		return list.get(rng(list.size() - 1, random));
	}

	public static <T> List<T> getRandomN(List<T> array, int elements) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();
		Random random = new Random(System.currentTimeMillis());

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = rng(aux.size() - 1, random);

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
			int index = rng(aux.size() - 1, random);

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
			int index = rng(aux.size() - 1, random);

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

	public static String replaceWith(String source, Map<String, String> replaces) {
		AtomicReference<String> toChange = new AtomicReference<>();
		for (Map.Entry<String, String> entry : replaces.entrySet()) {
			String k = entry.getKey();
			String v = entry.getValue();
			toChange.set(source.replace(k, v));
		}
		return toChange.get();
	}

	public static byte[] getBytes(BufferedImage image) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {
				ImageIO.write(image, "jpg", bos);
			}

			return baos.toByteArray();
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			return new byte[0];
		}
	}

	public static byte[] getBytes(BufferedImage image, String encoding) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {
				ImageIO.write(image, encoding, bos);
			}

			return baos.toByteArray();
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			return new byte[0];
		}
	}

	public static byte[] getBytes(BufferedImage image, String encode, float compression) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {
				ImageWriter writer = ImageIO.getImageWritersByFormatName(encode).next();
				ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
				writer.setOutput(ios);

				ImageWriteParam param = writer.getDefaultWriteParam();
				if (param.canWriteCompressed()) {
					param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					param.setCompressionQuality(compression);
				}

				writer.write(null, new IIOImage(image, null, null), param);
			}


			return baos.toByteArray();
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			return new byte[0];
		}
	}

	public static void spawnKawaipon(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getRatelimit().containsKey("kawaipon_" + gc.getGuildId())) return;

		double cardBuff = getBuffMult(gc, BuffType.CARD);
		double foilBuff = getBuffMult(gc, BuffType.FOIL);

		if (chance((3 - clamp(channel.getGuild().getMemberCount() / 5000, 0, 1)) * cardBuff)) {
			Main.getInfo().getRatelimit().put("kawaipon_" + gc.getGuildId(), true, 1, TimeUnit.MINUTES);

			List<org.apache.commons.math3.util.Pair<KawaiponRarity, Double>> odds = new ArrayList<>();
			for (KawaiponRarity kr : KawaiponRarity.validValues()) {
				odds.add(org.apache.commons.math3.util.Pair.create(kr, Math.pow(2, 5 - kr.getIndex())));
			}

			KawaiponRarity kr = getRandom(odds);

			List<Card> cards = CardDAO.getCardsByRarity(kr);
			Card c = getRandomEntry(cards);
			boolean foil = chance(0.5 * foilBuff);
			KawaiponCard kc = new KawaiponCard(c, foil);
			BufferedImage img = c.drawCard(foil);

			EmbedBuilder eb = new EmbedBuilder()
					.setAuthor("Uma carta " + c.getRarity().toString().toUpperCase(Locale.ROOT) + " Kawaipon apareceu neste servidor!")
					.setTitle(kc.getName() + " (" + c.getAnime().toString() + ")")
					.setColor(colorThief(img))
					.setFooter("Digite `" + gc.getPrefix() + "coletar` para adquirir esta carta (necessário: " + separate(c.getRarity().getIndex() * BASE_CARD_PRICE * (foil ? 2 : 1)) + " CR).", null);

			if (gc.isSmallCards())
				eb.setThumbnail("attachment://kawaipon.png");
			else
				eb.setImage("attachment://kawaipon.png");

			try {
				if (gc.getKawaiponChannel() == null) {
					channel.sendMessageEmbeds(eb.build()).addFile(writeAndGet(img, "kp_" + c.getId(), "png"), "kawaipon.png")
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, Helper::doNothing);
				} else {
					TextChannel tc = gc.getKawaiponChannel();

					if (tc == null) {
						gc.setKawaiponChannel(null);
						GuildDAO.updateGuildSettings(gc);
						channel.sendMessageEmbeds(eb.build()).addFile(writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
								.delay(1, TimeUnit.MINUTES)
								.flatMap(Message::delete)
								.queue(null, Helper::doNothing);
					} else {
						tc.sendMessageEmbeds(eb.build()).addFile(writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
								.delay(1, TimeUnit.MINUTES)
								.flatMap(Message::delete)
								.queue(null, Helper::doNothing);
					}
				}

				Main.getInfo().getCurrentCard().put(channel.getGuild().getId(), kc);
			} catch (IllegalStateException ignore) {
			}
		}
	}

	public static void forceSpawnKawaipon(GuildConfig gc, TextChannel channel, User user, AddedAnime anime, boolean foil) {
		double foilBuff = getBuffMult(gc, BuffType.FOIL);
		List<Card> cards;

		if (anime != null) {
			List<Card> cds = CardDAO.getCardsByAnime(anime.getName());
			Set<KawaiponRarity> rarities = cds.stream()
					.map(Card::getRarity)
					.collect(Collectors.toSet());

			List<org.apache.commons.math3.util.Pair<KawaiponRarity, Double>> odds = new ArrayList<>();
			for (KawaiponRarity kr : KawaiponRarity.validValues()) {
				if (!rarities.contains(kr)) continue;

				odds.add(org.apache.commons.math3.util.Pair.create(kr, Math.pow(2, 5 - kr.getIndex())));
			}

			KawaiponRarity kr = getRandom(odds);

			cards = cds.stream().filter(c -> c.getRarity() == kr).collect(Collectors.toList());
		} else {
			List<org.apache.commons.math3.util.Pair<KawaiponRarity, Double>> odds = new ArrayList<>();
			for (KawaiponRarity kr : KawaiponRarity.validValues()) {
				odds.add(org.apache.commons.math3.util.Pair.create(kr, Math.pow(2, 5 - kr.getIndex())));
			}

			cards = CardDAO.getCardsByRarity(getRandom(odds));
		}

		Card c = getRandomEntry(cards);
		foil = foil || chance(0.5 * foilBuff);
		KawaiponCard kc = new KawaiponCard(c, foil);
		BufferedImage img = c.drawCard(foil);

		EmbedBuilder eb = new EmbedBuilder()
				.setAuthor(user.getName() + " invocou uma carta " + c.getRarity().toString().toUpperCase(Locale.ROOT) + " neste servidor!")
				.setTitle(kc.getName() + " (" + c.getAnime().toString() + ")")
				.setColor(colorThief(img))
				.setFooter("Digite `" + gc.getPrefix() + "coletar` para adquirir esta carta (necessário: " + separate(c.getRarity().getIndex() * BASE_CARD_PRICE * (foil ? 2 : 1)) + " CR).", null);

		if (gc.isSmallCards())
			eb.setThumbnail("attachment://kawaipon.png");
		else
			eb.setImage("attachment://kawaipon.png");

		try {
			if (gc.getKawaiponChannel() == null) {
				channel.sendMessageEmbeds(eb.build()).addFile(writeAndGet(img, "kp_" + c.getId(), "png"), "kawaipon.png")
						.delay(1, TimeUnit.MINUTES)
						.flatMap(Message::delete)
						.queue(null, Helper::doNothing);
			} else {
				TextChannel tc = gc.getKawaiponChannel();

				if (tc == null) {
					gc.setKawaiponChannel(null);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessageEmbeds(eb.build()).addFile(writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, Helper::doNothing);
				} else {
					tc.sendMessageEmbeds(eb.build()).addFile(writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, Helper::doNothing);
				}
			}

			Main.getInfo().getCurrentCard().put(channel.getGuild().getId(), kc);
		} catch (IllegalStateException ignore) {
		}
	}

	public static void spawnDrop(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getRatelimit().containsKey("drop_" + gc.getGuildId())) return;

		double dropBuff = getBuffMult(gc, BuffType.DROP);

		if (chance((2.5 - clamp(channel.getGuild().getMemberCount() * 0.75f / 5000, 0, 0.75)) * dropBuff)) {
			Main.getInfo().getRatelimit().put("drop_" + gc.getGuildId(), true, 1, TimeUnit.MINUTES);

			DynamicParameter dp = DynamicParameterDAO.getParam("golden_tickets");
			int rem = NumberUtils.toInt(dp.getValue());

			Prize<?> drop;
			int type = rng(1000);

			if (type >= 995)
				drop = new FieldDrop();
			else if (type >= 985 && rem < 5)
				drop = new TicketDrop();
			else if (type >= 975)
				drop = new EvogearDrop();
			else if (type >= 900)
				drop = new ChampionDrop();
			else
				drop = new CreditDrop();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setThumbnail("https://i.pinimg.com/originals/86/c0/f4/86c0f4d0f020c3f819a532873ef33704.png")
					.setTitle("Um drop apareceu neste servidor!")
					.addField("Conteúdo:", drop.toString(), true)
					.addField("Código captcha:", drop.getCaptcha(), true)
					.setFooter("Digite `" + gc.getPrefix() + "abrir` para receber o prêmio (requisito: " + drop.getRequirement().getKey() + ").", null);

			try {
				if (gc.getDropChannel() == null) {
					channel.sendMessageEmbeds(eb.build())
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, Helper::doNothing);
				} else {
					TextChannel tc = gc.getDropChannel();

					if (tc == null) {
						gc.setDropChannel(null);
						GuildDAO.updateGuildSettings(gc);
						channel.sendMessageEmbeds(eb.build())
								.delay(1, TimeUnit.MINUTES)
								.flatMap(Message::delete)
								.queue(null, Helper::doNothing);
					} else {
						tc.sendMessageEmbeds(eb.build())
								.delay(1, TimeUnit.MINUTES)
								.flatMap(Message::delete)
								.queue(null, Helper::doNothing);
					}
				}

				Main.getInfo().getCurrentDrop().put(channel.getGuild().getId(), drop);
			} catch (IllegalStateException ignore) {
			}
		}
	}

	public static void spawnPadoru(GuildConfig gc, TextChannel channel) {
		String padoru = ShiroInfo.RESOURCES_URL + "/assets/padoru_padoru.gif";
		if (Main.getInfo().getSpecialEvent().containsKey(gc.getGuildId())) return;

		if (chance(0.1 - clamp(channel.getGuild().getMemberCount() * 0.08 / 5000, 0, 0.08))) {
			Main.getInfo().getSpecialEvent().put(gc.getGuildId(), true);

			try {
				TextChannel tc = getOr(gc.getDropChannel(), channel);
				Webhook wh = getOrCreateWebhook(tc, "Shiro");
				WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();

				WebhookMessageBuilder wmb = new WebhookMessageBuilder();
				wmb.setUsername("Nero (Evento Padoru)");
				wmb.setAvatarUrl(ShiroInfo.NERO_AVATAR.formatted(1));

				List<Prize<?>> prizes = new ArrayList<>();
				for (int i = 0; i < 6; i++) {
					int type = rng(1000);

					if (type >= 997 && prizes.stream().noneMatch(p -> p.getClass() == PadoruDrop.class))
						prizes.add(new PadoruDrop());
					else if (type >= 995)
						prizes.add(new FieldDrop());
					else if (type >= 975)
						prizes.add(new EvogearDrop());
					else if (type >= 900)
						prizes.add(new ChampionDrop());
					else
						prizes.add(new CreditDrop());
				}

				ColorlessWebhookEmbedBuilder web = new ColorlessWebhookEmbedBuilder()
						.setDescription("""
								**Hashire sori yo**
								**Kaze no you ni**
								**Tsukimihara wo...**
								""")
						.setThumbnailUrl(ShiroInfo.RESOURCES_URL + "/assets/padoru.gif")
						.setTitle("Nero Claudius apareceu trazendo presentes neste servidor!");

				for (int i = 0; i < prizes.size(); i++) {
					Prize<?> prize = prizes.get(i);
					web.addField("Presente " + (i + 1) + ":", prize.toString(), true);
				}

				web.setFooter("Complete a música para participar do sorteio dos prêmios.", null);

				Set<String> users = new HashSet<>();
				SimpleMessageListener sml = new SimpleMessageListener(channel) {
					@Override
					public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
						String msg = event.getMessage().getContentRaw();
						User author = event.getAuthor();
						if (msg.equalsIgnoreCase("PADORU PADORU") && !author.isBot() && users.add(author.getId())) {
							Emote e = Main.getShiroShards().getEmoteById("787012642501689344");
							if (e != null) event.getMessage().addReaction(e).queue();
						}
					}
				};

				ShiroInfo.getShiroEvents().addHandler(channel.getGuild(), sml);
				Consumer<Message> act = msg -> {
					if (users.size() > 0) {
						List<String> ids = List.copyOf(users);
						User u = Main.getInfo().getUserByID(getRandomEntry(ids));

						ColorlessWebhookEmbedBuilder neb = new ColorlessWebhookEmbedBuilder()
								.setImageUrl(padoru);

						for (int i = 0; i < prizes.size(); i++) {
							Prize<?> prize = prizes.get(i);
							neb.addField("Presente " + (i + 1) + ":", prize.toString(u), true);
							prize.award(u);
						}

						wc.send(wmb.resetEmbeds()
								.setContent("Decidi que " + u.getAsMention() + " merece os presentes!")
								.addEmbeds(neb.build())
								.build());
					} else {
						wc.send(wmb.resetEmbeds()
								.setContent("Decidi que ninguém merece os presentes!")
								.build());
					}

					wc.close();
					sml.close();
				};

				ReadonlyMessage rm = wc.send(wmb.addEmbeds(web.build()).build()).get();
				if (gc.getDropChannel() == null) {
					tc.retrieveMessageById(rm.getId())
							.delay(1, TimeUnit.MINUTES)
							.queue(msg -> {
								msg.delete().queue(null, Helper::doNothing);
								act.accept(msg);
							});
				} else {
					tc.retrieveMessageById(rm.getId())
							.delay(1, TimeUnit.MINUTES)
							.queue(msg -> {
								msg.delete().queue(null, Helper::doNothing);
								act.accept(msg);
							});
				}
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException ignore) {
			}
		}
	}

	public static void spawnUsaTan(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getSpecialEvent().containsKey(gc.getGuildId())) return;

		if (chance(0.15 - clamp(channel.getGuild().getMemberCount() * 0.5 / 5000, 0, 0.05))) {
			Main.getInfo().getSpecialEvent().put(gc.getGuildId(), true);

			try {
				TextChannel tc = getOr(gc.getDropChannel(), channel);
				Webhook wh = getOrCreateWebhook(tc, "Shiro");
				WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();

				WebhookMessageBuilder wmb = new WebhookMessageBuilder();
				wmb.setUsername("Usa-tan (Evento Páscoa)");
				wmb.setAvatarUrl(ShiroInfo.USATAN_AVATAR.formatted(1));

				Emote egg = Main.getShiroShards().getEmoteById(TagIcons.EASTER_EGG.getId(0));
				assert egg != null;

				List<Prize<?>> prizes = new ArrayList<>();
				for (int i = 0; i < 6; i++) {
					int type = rng(1000);

					if (type >= 995)
						prizes.add(new FieldDrop());
					else if (type >= 975)
						prizes.add(new EvogearDrop());
					else if (type >= 900)
						prizes.add(new ChampionDrop());
					else
						prizes.add(new CreditDrop());
				}

				ColorlessWebhookEmbedBuilder web = new ColorlessWebhookEmbedBuilder()
						.setDescription("Hehe, será que você consegue achar o ovo de páscoa que eu escondi neste canal?")
						.setThumbnailUrl(egg.getImageUrl())
						.setTitle("Usa-tan apareceu trazendo presentes neste servidor!");

				for (int i = 0; i < prizes.size(); i++) {
					Prize<?> prize = prizes.get(i);
					web.addField("Ovo " + (i + 1) + ":", prize.toString(), true);
				}

				web.setFooter("Enconte a reação de ovo de páscoa escondido em uma das mensagens neste canal.", null);

				AtomicBoolean found = new AtomicBoolean(false);
				AtomicBoolean finished = new AtomicBoolean(false);
				Runnable act = () -> {
					if (finished.get()) return;
					wc.send(wmb.resetEmbeds()
							.setContent("Ninguem encontrou o ovo de páscoa a tempo!")
							.build());
					wc.close();
					finished.set(true);
				};

				List<Message> hist = Pages.subGet(tc.getHistory().retrievePast(100));

				if (hist.size() == 0) return;

				Message m = getRandomEntry(hist);
				Pages.buttonize(m, Collections.singletonMap(
						parseEmoji(egg.getId()), wrapper -> {
							if (finished.get()) return;

							ColorlessWebhookEmbedBuilder neb = new ColorlessWebhookEmbedBuilder();
							for (int i = 0; i < prizes.size(); i++) {
								Prize<?> prize = prizes.get(i);
								neb.addField("Ovo " + (i + 1) + ":", prize.toString(wrapper.getUser()), true);
								prize.award(wrapper.getUser());
							}

							wc.send(wmb.resetEmbeds()
									.setContent(wrapper.getUser().getAsMention() + " encontrou o ovo de páscoa!")
									.addEmbeds(neb.build())
									.build());
							wc.close();
							found.set(true);
							finished.set(true);
						}
				), ShiroInfo.USE_BUTTONS, false, 2, TimeUnit.MINUTES);

				ReadonlyMessage rm = wc.send(wmb.addEmbeds(web.build()).build()).get();
				if (gc.getDropChannel() == null) {
					tc.retrieveMessageById(rm.getId())
							.delay(2, TimeUnit.MINUTES)
							.queue(msg -> {
								msg.delete().queue(null, Helper::doNothing);
								if (!found.get()) act.run();
							}, Helper::doNothing);
				} else {
					tc.retrieveMessageById(rm.getId())
							.delay(2, TimeUnit.MINUTES)
							.queue(msg -> {
								msg.delete().queue(null, Helper::doNothing);
								if (!found.get()) act.run();
							}, Helper::doNothing);
				}
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException ignore) {
			}
		}
	}

	public static boolean chance(double percentage) {
		return rng(100d) < percentage;
	}

	public static void drawRotated(Graphics2D g2d, BufferedImage bi, int x, int y, double deg) {
		AffineTransform old = g2d.getTransform();
		g2d.rotate(Math.toRadians(deg), x, y);
		g2d.drawImage(bi, 0, 0, null);
		g2d.setTransform(old);
	}

	public static void writeRotated(Graphics2D g2d, String s, int x, int y, double deg) {
		AffineTransform old = g2d.getTransform();
		g2d.rotate(Math.toRadians(deg), x, y);
		g2d.drawString(s, 0, -10);
		g2d.setTransform(old);
	}

	public static void darkenImage(float fac, BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getHeight(); x++) {
				Color rgb = new Color(image.getRGB(x, y), true);
				int r = clamp(Math.round(rgb.getRed() * fac), 0, 255);
				int g = clamp(Math.round(rgb.getGreen() * fac), 0, 255);
				int b = clamp(Math.round(rgb.getBlue() * fac), 0, 255);
				image.setRGB(x, y, new Color(r, g, b, rgb.getAlpha()).getRGB());
			}
		}
	}

	public static BufferedImage scaleImage(BufferedImage image, int w, int h) {
		double thumbRatio = (double) w / (double) h;
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		double aspectRatio = (double) imageWidth / (double) imageHeight;

		if (thumbRatio > aspectRatio) {
			h = (int) (w / aspectRatio);
		} else {
			w = (int) (h * aspectRatio);
		}

		BufferedImage newImage = new BufferedImage(w, h, image.getType());
		Graphics2D g2d = newImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(image, 0, 0, w, h, null);
		g2d.dispose();

		return newImage;
	}

	public static BufferedImage scaleImage(BufferedImage image, int prcnt) {
		int w = image.getWidth() / prcnt;
		int h = image.getHeight() / prcnt;

		BufferedImage newImage = new BufferedImage(w, h, image.getType());
		Graphics2D g2d = newImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(image, 0, 0, w, h, null);
		g2d.dispose();

		return newImage;
	}

	public static BufferedImage scaleAndCenterImage(BufferedImage image, int w, int h) {
		image = scaleImage(image, w, h);

		int offX = Math.min((image.getWidth() - w) / -2, 0);
		int offY = Math.min((image.getHeight() - h) / -2, 0);

		BufferedImage newImage = new BufferedImage(w, h, image.getType());
		Graphics2D g2d = newImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(image, offX, offY, null);
		g2d.dispose();

		return newImage;
	}

	public static BufferedImage removeAlpha(BufferedImage input) {
		BufferedImage bi = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g2d.drawImage(input, 0, 0, null);
		g2d.dispose();

		return bi;
	}

	public static String getFileType(String url) {
		try {
			HttpHead req = new HttpHead(url);

			try (CloseableHttpResponse res = ShiroInfo.getHttp().execute(req)) {
				return res.getFirstHeader("Content-Type").getValue();
			}
		} catch (JsonDataException | IllegalStateException | IOException e) {
			return null;
		}
	}

	public static String hash(byte[] bytes, String encoding) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance(encoding).digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			return "";
		}
	}

	public static String hash(String value, String encoding) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance(encoding).digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			return "";
		}
	}

	public static String hmac(byte[] bytes, byte[] key, String encoding) {
		try {
			Mac hmac = Mac.getInstance("Hmac" + encoding);
			hmac.init(new SecretKeySpec(key, hmac.getAlgorithm()));
			return Hex.encodeHexString(hmac.doFinal(bytes));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			return "";
		}
	}

	public static boolean monoDigit(String word, String ch) {
		if (word.length() == 0) return false;
		for (String s : word.split("")) {
			if (!s.equals(ch)) return false;
		}

		return true;
	}

	public static String toDuration(long millis) {
		return String.format("%d:%02d:%02d",
				(int) ((millis / (1000 * 60 * 60))),
				(int) ((millis / (1000 * 60)) % 60),
				(int) (millis / 1000) % 60
		);
	}

	public static CardStatus checkStatus(Kawaipon kp) {
		int normalCount = kp.getNormalCards().size();
		int foilCount = kp.getFoilCards().size();
		int total = (int) CardDAO.getTotalCards();

		if (normalCount + foilCount == total * 2) return CardStatus.NO_CARDS;
		else if (foilCount == total) return CardStatus.NORMAL_CARDS;
		else if (normalCount == total) return CardStatus.FOIL_CARDS;
		else return CardStatus.ALL_CARDS;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void keepMaximumNFiles(File folder, int maximum) {
		if (!folder.isDirectory()) return;
		List<Pair<File, FileTime>> files = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
				.map(f -> {
					FileTime time;
					try {
						time = Files.getLastModifiedTime(f.toPath());
					} catch (IOException e) {
						time = null;
					}
					return Pair.of(f, time);
				})
				.collect(Collectors.toList());

		files.removeIf(p -> p.getRight() == null);

		if (files.size() <= maximum) return;

		files.sort(Comparator.comparing(Pair::getRight));
		while (files.size() > maximum) {
			files.remove(0).getLeft().delete();
		}
	}

	public static JSONObject findJson(String text) {
		String json = extract(text, "\\{.*}");

		if (json == null) return null;
		else return new JSONObject(json);
	}

	public static String noCopyPaste(String input) {
		return String.join(ANTICOPY, input.split(""));
	}

	public static <T> T getRandom(List<org.apache.commons.math3.util.Pair<T, Double>> values) {
		EnumeratedDistribution<T> ed = new EnumeratedDistribution<>(
				values.stream()
						.sorted(Comparator.comparingDouble(org.apache.commons.math3.util.Pair::getValue))
						.map(p -> p.getValue() < 0 ? org.apache.commons.math3.util.Pair.create(p.getFirst(), 0d) : p)
						.collect(Collectors.toList())
		);

		return ed.sample();
	}

	public static Deck getDailyDeck() {
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
		long seed = Long.parseLong("" + today.getYear() + today.getMonthValue() + today.getDayOfMonth());
		Deck dk = new Deck();

		dk.setChampions(getRandomN(CardDAO.getAllChampions(false), 30, 3, seed));
		dk.setEquipments(getRandomN(CardDAO.getAllAvailableEquipments(), 6, 3, seed));
		dk.setFields(getRandomN(CardDAO.getAllAvailableFields(), 1, 3, seed));

		return dk;
	}

	public static double log(double value, double base) {
		return Math.log(value) / Math.log(base);
	}

	public static String getShortenedValue(long value, int forEach) {
		if (value == 0) return String.valueOf(value);
		int times = (int) Math.floor(log(value, forEach));
		String reduced = roundToString(value / Math.pow(forEach, times), 2);


		return reduced + StringUtils.repeat("k", times);
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

	public static void broadcast(String message, TextChannel channel) {
		List<WebhookClient> clients = new ArrayList<>();
		List<GuildConfig> gcs = GuildDAO.getAlertChannels();

		int success = 0;
		int failed = 0;

		for (GuildConfig gc : gcs) {
			Guild g = Main.getInfo().getGuildByID(gc.getGuildId());
			if (g == null) continue;
			try {
				TextChannel c = gc.getAlertChannel();
				if (c != null && c.canTalk()) {
					Webhook wh = getOrCreateWebhook(c, "Notificações Shiro");
					if (wh == null) failed++;

					else {
						WebhookClientBuilder wcb = new WebhookClientBuilder(wh.getUrl());
						clients.add(wcb.build());
						success++;
					}
				} else failed++;
			} catch (Exception e) {
				failed++;
			}
		}

		WebhookMessageBuilder wmb = new WebhookMessageBuilder();
		wmb.setUsername("Stephanie (Notificações Shiro)");

		int v;
		if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.OCTOBER)
			v = 2;
		else
			v = 1;

		wmb.setAvatarUrl(ShiroInfo.STEPHANIE_AVATAR.formatted(v));
		wmb.setContent(message);
		WebhookCluster cluster = new WebhookCluster(clients);
		cluster.broadcast(wmb.build());
		if (channel != null)
			channel.sendMessage(":loud_sound: | Sucesso: " + success + "\n:mute: | Falha: " + failed).queue();
	}

	public static void applyMask(BufferedImage source, BufferedImage mask, int channel) {
		BufferedImage newMask = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = newMask.createGraphics();
		g2d.drawImage(mask, 0, 0, newMask.getWidth(), newMask.getHeight(), null);
		g2d.dispose();

		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int[] rgb = unpackRGB(source.getRGB(x, y));

				int fac = unpackRGB(newMask.getRGB(x, y))[channel];
				source.setRGB(
						x,
						y,
						packRGB(fac, rgb[1], rgb[2], rgb[3])
				);
			}
		}
	}

	public static void applyMask(BufferedImage source, BufferedImage mask, int channel, boolean hasAlpha) {
		BufferedImage newMask = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = newMask.createGraphics();
		g2d.drawImage(mask, 0, 0, newMask.getWidth(), newMask.getHeight(), null);
		g2d.dispose();

		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int[] rgb = unpackRGB(source.getRGB(x, y));

				int fac;
				if (hasAlpha) {
					fac = Math.min(rgb[0], unpackRGB(newMask.getRGB(x, y))[channel + 1]);
				} else
					fac = unpackRGB(newMask.getRGB(x, y))[channel + 1];
				source.setRGB(
						x,
						y,
						packRGB(fac, rgb[1], rgb[2], rgb[3])
				);
			}
		}
	}

	public static int[] unpackRGB(int rgb) {
		return new int[]{
				(rgb >> 24) & 0xFF,
				(rgb >> 16) & 0xFF,
				(rgb >> 8) & 0xFF,
				rgb & 0xFF
		};
	}

	public static int packRGB(int a, int r, int g, int b) {
		return a << 24 | r << 16 | g << 8 | b;
	}

	public static int packRGB(int[] argb) {
		return argb[0] << 24 | argb[1] << 16 | argb[2] << 8 | argb[3];
	}

	public static BufferedImage toColorSpace(BufferedImage in, int type) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), type);
		Graphics2D g2d = out.createGraphics();
		g2d.drawImage(in, 0, 0, null);
		g2d.dispose();
		return out;
	}

	public static boolean hasEmote(String text) {
		for (String word : text.split(" ")) {
			if (word.startsWith(":") && word.endsWith(":")) {
				if (ShiroInfo.getEmoteLookup().containsKey(word)) return true;
			}
		}

		return false;
	}

	public static String getFancyNumber(int number) {
		String sNumber = String.valueOf(number);
		StringBuilder sb = new StringBuilder();
		for (char c : sNumber.toCharArray())
			sb.append(getNumericEmoji(Integer.parseInt(String.valueOf(c))));

		return sb.toString();
	}

	public static String bugText(String text) {
		return String.join(ANTICOPY, text.split(""));
	}

	public static String getRegionalIndicator(int i) {
		return new String(new char[]{"\uD83C\uDDE6".toCharArray()[0], (char) ("\uD83C\uDDE6".toCharArray()[1] + i)});
	}

	public static String getNumericEmoji(int i) {
		return i + "⃣";
	}

	public static <T> T getNext(T current, List<T> sequence) {
		int index = sequence.indexOf(current);
		return index == -1 ? null : sequence.get(Math.min(index + 1, sequence.size() - 1));
	}

	@SafeVarargs
	public static <T> T getNext(T current, T... sequence) {
		int index = ArrayUtils.indexOf(sequence, current);
		return index == -1 ? null : sequence[Math.min(index + 1, sequence.length - 1)];
	}

	public static <T> T getPrevious(T current, List<T> sequence) {
		int index = sequence.indexOf(current);
		return index == -1 ? null : sequence.get(Math.max(index - 1, 0));
	}

	@SafeVarargs
	public static <T> T getPrevious(T current, T... sequence) {
		int index = ArrayUtils.indexOf(sequence, current);
		return index == -1 ? null : sequence[Math.max(index - 1, 0)];
	}

	public static String atob(BufferedImage bi, String encoding) {
		return atob(getBytes(bi, encoding));
	}

	public static String atob(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	public static BufferedImage btoa(String b64) {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)))) {
			return ImageIO.read(bais);
		} catch (IOException | NullPointerException e) {
			return null;
		}
	}

	public static byte[] btoc(String b64) {
		return Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8));
	}

	public static String separate(Object value) {
		try {
			Number n = value instanceof Number nb ? nb : NumberUtils.createNumber(String.valueOf(value));
			DecimalFormat df = new DecimalFormat();
			df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));
			df.setGroupingSize(3);

			return df.format(n);
		} catch (NumberFormatException e) {
			return String.valueOf(value);
		}
	}

	public static String replaceTags(String text, User user, Guild guild, Message msg) {
		Map<String, String> reps = new HashMap<>() {{
			if (user != null) {
				put("%user%", user.getAsMention());
				put("%user.id%", user.getId());
				put("%user.name%", user.getName());
				put("%user.created%", TIMESTAMP.formatted(user.getTimeCreated().toEpochSecond()));
				put("%user.raids%", Helper.separate(RaidDAO.getUserRaids(user.getId())));
			}

			if (guild != null) {
				put("%guild%", guild.getName());
				put("%guild.count%", Helper.separate(guild.getMemberCount()));
			}

			if (msg != null) {
				put("%message%", msg.getContentRaw());
			}
		}};

		for (Map.Entry<String, String> rep : reps.entrySet()) {
			text = text.replace(rep.getKey(), rep.getValue());
		}

		return makeEmoteFromMention(text);
	}

	public static boolean isPureMention(String msg) {
		return msg.matches("<(@|@!)\\d+>");
	}

	public static boolean isPinging(Message msg, String id) {
		User u = Main.getInfo().getUserByID(id);
		return msg.isMentioned(u, Message.MentionType.USER);
	}

	public static <T> List<T> getIndexes(List<T> list, int... indexes) {
		List<T> out = new ArrayList<>();
		for (int index : indexes) {
			if (index < list.size()) out.add(list.get(index));
		}

		return out;
	}

	@SafeVarargs
	public static <T> boolean isTwice(T... objs) {
		List<T> elements = List.of(objs);
		for (T obj : elements) {
			if (Collections.frequency(elements, obj) > 1)
				return true;
		}
		return false;
	}

	@SafeVarargs
	public static <T> Triple<List<T>, Double, List<T>> balanceSides(ToIntFunction<T> extractor, T... objs) {
		LinkedList<T> elements = new LinkedList<>(Arrays.asList(objs));
		elements.sort(Comparator.comparingInt(extractor));

		Duo<List<T>, Integer> firstGroup = new Duo<>(new ArrayList<>(), 0);
		Duo<List<T>, Integer> secondGroup = new Duo<>(new ArrayList<>(), 0);

		boolean first = true;
		while (elements.size() > 0) {
			T lesser = elements.removeFirst();
			T higher = elements.removeLast();

			if (first) {
				firstGroup.getLeft().add(lesser);
				firstGroup.getLeft().add(higher);
				firstGroup.setRight((firstGroup.getRight() + extractor.applyAsInt(lesser) + extractor.applyAsInt(higher)) / 3);

				first = false;
			} else {
				secondGroup.getLeft().add(lesser);
				secondGroup.getLeft().add(higher);
				secondGroup.setRight((secondGroup.getRight() + extractor.applyAsInt(lesser) + extractor.applyAsInt(higher)) / 3);

				first = true;
			}
		}

		return Triple.of(firstGroup.getLeft(), Math.abs(firstGroup.getRight() - secondGroup.getRight()) / (double) (firstGroup.getRight() + secondGroup.getRight()), secondGroup.getLeft());
	}

	public static long getAverageMMR(String... ids) {
		return Math.round(Arrays.stream(ids)
				.map(MatchMakingRatingDAO::getMMR)
				.mapToLong(MatchMakingRating::getMMR)
				.average()
				.orElse(0));
	}

	public static List<Integer> getNumericList(int min, int max) {
		List<Integer> values = new ArrayList<>();
		for (int i = min; i < max; i++) {
			values.add(i);
		}
		return values;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static String serveImage(byte[] bytes) {
		String hash = hash(System.currentTimeMillis() + hash(bytes, "MD5"), "MD5");
		File f = new File(Main.getInfo().getTemporaryFolder(), hash);

		try {
			f.createNewFile();
			FileUtils.writeByteArrayToFile(f, bytes);
			return ShiroInfo.IMAGE_ENDPOINT.formatted(hash);
		} catch (IOException e) {
			return null;
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static String serveImage(byte[] bytes, String hash) {
		File f = new File(Main.getInfo().getTemporaryFolder(), hash);

		try {
			f.createNewFile();
			FileUtils.writeByteArrayToFile(f, bytes);
			return ShiroInfo.IMAGE_ENDPOINT.formatted(hash);
		} catch (IOException e) {
			return null;
		}
	}

	public static <T> MessageAction generateStore(User u, TextChannel chn, String title, String desc, List<T> items, Function<T, MessageEmbed.Field> fieldExtractor) {
		Account acc = AccountDAO.getAccount(u.getId());
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(title)
				.setDescription(desc)
				.setFooter("""
						💰 CR: %s
						♦️ Gemas: %s
						""".formatted(separate(acc.getBalance()), separate(acc.getGems())));

		for (T item : items) {
			eb.addField(fieldExtractor.apply(item));
		}

		return chn.sendMessageEmbeds(eb.build());
	}

	public static <T> MessageAction generateStore(User u, TextChannel chn, String title, String desc, Color color, List<T> items, Function<T, MessageEmbed.Field> fieldExtractor) {
		Account acc = AccountDAO.getAccount(u.getId());
		EmbedBuilder eb = new EmbedBuilder()
				.setTitle(title)
				.setDescription(desc)
				.setColor(color)
				.setFooter("""
						💰 CR: %s
						♦️ Gemas: %s
						""".formatted(separate(acc.getBalance()), separate(acc.getGems())));

		for (T item : items) {
			eb.addField(fieldExtractor.apply(item));
		}

		return chn.sendMessageEmbeds(eb.build());
	}

	public static String buildFrame(String sketch) {
		Map<String, String> replacements = new HashMap<>() {{
			put("─", "<:horizontal_top:747882840351572020>");
			put("┌", "<:corner_down_right:747882840451973170>");
			put("┐", "<:corner_down_left:747882840380932286>");
			put("└", "<:corner_up_right:747882840439652522>");
			put("┘", "<:corner_up_left:747882840326406246>");
			put("┬", "<:cross_down:747882840477138994>");
			put("┴", "<:cross_up:747882840489853000>");
			put("═", "<:horizontal_bottom:747882840565350430>");
			put("┤", "<:vertical_right:747882840569544714>");
			put("├", "<:vertical_left:747882840414486571>");
			put("│", "<:vertical:747883406632943669>");
			put("%", "%s");
		}};

		StringBuilder sb = new StringBuilder();
		for (char c : sketch.toCharArray())
			sb.append(replacements.getOrDefault(String.valueOf(c), String.valueOf(c)));

		return sb.toString();
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

	public static boolean findParam(String[] args, String... param) {
		return Arrays.stream(args).anyMatch(s -> equalsAny(s, param));
	}

	public static int subtract(int a, int b) {
		return a - b;
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

	public static XYChart buildXYChart(String title, Pair<String, String> axis, List<Color> colors) {
		XYChart chart = new XYChartBuilder()
				.width(1920)
				.height(1080)
				.title(title)
				.xAxisTitle(axis.getLeft())
				.yAxisTitle(axis.getRight())
				.build();

		chart.getStyler()
				.setPlotGridLinesColor(new Color(64, 68, 71))
				.setAxisTickLabelsColor(Color.WHITE)
				.setAnnotationsFontColor(Color.WHITE)
				.setChartFontColor(Color.WHITE)
				.setHasAnnotations(true)
				.setLegendPosition(Styler.LegendPosition.InsideNE)
				.setSeriesColors(colors.toArray(Color[]::new))
				.setPlotBackgroundColor(new Color(32, 34, 37))
				.setChartBackgroundColor(new Color(16, 17, 20))
				.setLegendBackgroundColor(new Color(16, 17, 20, 100))
				.setSeriesLines(Collections.nCopies(6, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)).toArray(BasicStroke[]::new));

		return chart;
	}

	public static CategoryChart buildBarChart(String title, Pair<String, String> axis, List<Color> colors) {
		CategoryChart chart = new CategoryChartBuilder()
				.width(1920)
				.height(1080)
				.title(title)
				.xAxisTitle(axis.getLeft())
				.yAxisTitle(axis.getRight())
				.build();

		chart.getStyler()
				.setPlotGridLinesColor(new Color(64, 68, 71))
				.setAxisTickLabelsColor(Color.WHITE)
				.setAnnotationsFontColor(Color.WHITE)
				.setChartFontColor(Color.WHITE)
				.setHasAnnotations(true)
				.setLegendPosition(Styler.LegendPosition.InsideNE)
				.setSeriesColors(colors.toArray(Color[]::new))
				.setPlotBackgroundColor(new Color(32, 34, 37))
				.setChartBackgroundColor(new Color(16, 17, 20))
				.setLegendBackgroundColor(new Color(16, 17, 20, 100));

		return chart;
	}

	public static OHLCChart buildOHLCChart(String title, Pair<String, String> axis, List<Color> colors) {
		OHLCChart chart = new OHLCChartBuilder()
				.width(1920)
				.height(1080)
				.title(title)
				.xAxisTitle(axis.getLeft())
				.yAxisTitle(axis.getRight())
				.build();

		chart.getStyler()
				.setPlotGridLinesColor(new Color(64, 68, 71))
				.setAxisTickLabelsColor(Color.WHITE)
				.setAnnotationsFontColor(Color.WHITE)
				.setChartFontColor(Color.WHITE)
				.setHasAnnotations(true)
				.setLegendPosition(Styler.LegendPosition.InsideNE)
				.setSeriesColors(colors.toArray(Color[]::new))
				.setPlotBackgroundColor(new Color(32, 34, 37))
				.setChartBackgroundColor(new Color(16, 17, 20))
				.setLegendBackgroundColor(new Color(16, 17, 20, 100))
				.setSeriesLines(Collections.nCopies(6, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)).toArray(BasicStroke[]::new));

		return chart;
	}

	public static String generateRandomHash(int length) {
		try {
			String method;

			if (length <= 0) return "";
			else if (length <= 32) method = "MD5";
			else if (length <= 40) method = "SHA-1";
			else if (length <= 64) method = "SHA-256";
			else if (length <= 128) method = "SHA-512";
			else return "";

			return Hex.encodeHexString(MessageDigest.getInstance(method).digest(SecureRandom.getSeed(length))).substring(0, length);
		} catch (NoSuchAlgorithmException e) {
			return "";
		}
	}

	public static URL getResource(Class<?> klass, String path) {
		URL url = klass.getClassLoader().getResource(path);
		if (url == null) throw new NullPointerException();
		else return url;
	}

	public static InputStream getResourceAsStream(Class<?> klass, String path) {
		InputStream is = klass.getClassLoader().getResourceAsStream(path);
		if (is == null) throw new NullPointerException();
		else return is;
	}

	public static BufferedImage getResourceAsImage(Class<?> klass, String path) {
		byte[] bytes = Main.getCacheManager().getResourceCache().computeIfAbsent(path, s -> {
			InputStream is = klass.getClassLoader().getResourceAsStream(path);

			if (is == null) return new byte[0];
			else {
				try {
					return getBytes(ImageIO.read(is), path.split("\\.")[1]);
				} catch (IOException e) {
					return new byte[0];
				}
			}
		});

		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
			//return Helper.toCompatibleImage(ImageIO.read(bais));
			return ImageIO.read(bais);
		} catch (IOException e) {
			return null;
		}
	}

	public static File getResourceAsFile(Class<?> klass, String path) {
		try {
			return new File(getResource(klass, path).toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	}

	public static int roundToBit(int value) {
		return 1 << (int) Math.round(log(value, 2));
	}

	public static int roundTrunc(int value, int mult) {
		return mult * Math.round((float) value / mult);
	}

	public static long roundTrunc(long value, int mult) {
		return mult * Math.round((double) value / mult);
	}

	public static int roundTrunc(float value, int mult) {
		return mult * Math.round(value / mult);
	}

	public static long roundTrunc(double value, int mult) {
		return mult * Math.round(value / mult);
	}

	public static File writeAndGet(BufferedImage bi, String name, String extension) {
		File tempFolder = Main.getInfo().getTemporaryFolder();
		File f = new File(tempFolder, name + "." + extension);

		try {
			ImageIO.write(bi, extension, f);
		} catch (IOException e) {
			try {
				ImageIO.write(bi, extension.equals("png") ? "jpg" : "png", f);
			} catch (IOException ignore) {
			}
		}

		return f;
	}

	public static File writeAndGet(BufferedImage bi, String name, String extension, File parent) {
		File f = new File(parent, name + "." + extension);

		try {
			ImageIO.write(bi, extension, f);
		} catch (IOException ignore) {
		}

		return f;
	}

	public static void forEachPixel(BufferedImage bi, BiConsumer<int[], Integer> act) {
		int x;
		int y;
		int i = 0;
		while (true) {
			x = i % bi.getWidth();
			y = i / bi.getWidth();

			if (x >= bi.getWidth() || y >= bi.getHeight()) break;

			act.accept(new int[]{x, y}, bi.getRGB(x, y));
			i++;
		}
	}

	public static void forEachFrame(List<BufferedImage> frames, Consumer<Graphics2D> act) {
		for (BufferedImage frame : frames) {
			Graphics2D g2d = frame.createGraphics();
			act.accept(g2d);
			g2d.dispose();
		}
	}

	public static CompletableFuture<Void> forEachFrame(List<BufferedImage> frames, ExecutorService exec, Consumer<Graphics2D> act) {
		NContract<Void> con = new NContract<>(frames.size());
		for (BufferedImage frame : frames) {
			exec.execute(() -> {
				Graphics2D g2d = frame.createGraphics();
				act.accept(g2d);
				g2d.dispose();

				con.addSignature(0, null);
			});
		}

		return con;
	}

	public static BufferedImage applyOverlay(BufferedImage in, BufferedImage overlay) {
		BufferedImage bi = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(in, 0, 0, null);
		g2d.drawImage(overlay, 0, 0, null);
		g2d.dispose();

		return bi;
	}

	public static int hip(int cat1, int cat2) {
		return (int) Math.round(Math.sqrt(Math.pow(cat1, 2) + Math.pow(cat2, 2)));
	}

	public static long hip(long cat1, long cat2) {
		return Math.round(Math.sqrt(Math.pow(cat1, 2) + Math.pow(cat2, 2)));
	}

	public static float hip(float cat1, float cat2) {
		return Math.round(Math.sqrt(Math.pow(cat1, 2) + Math.pow(cat2, 2)));
	}

	public static double hip(double cat1, double cat2) {
		return Math.round(Math.sqrt(Math.pow(cat1, 2) + Math.pow(cat2, 2)));
	}

	public static String getImageFrom(Message m) {
		if (!m.getAttachments().isEmpty()) {
			Message.Attachment att = m.getAttachments().get(0);
			if (att.isImage())
				return att.getUrl();
		} else if (!m.getEmbeds().isEmpty()) {
			MessageEmbed emb = m.getEmbeds().get(0);
			if (emb.getImage() != null)
				return emb.getImage().getProxyUrl();
		} else if (!m.getEmotes().isEmpty()) {
			Emote e = m.getEmotes().stream().findFirst().orElse(null);
			if (e != null)
				return m.getEmotes().get(0).getImageUrl();
		}

		return null;
	}

	public static <T extends Collection<String>> Function<T, String> properlyJoin() {
		return objs -> {
			List<String> ls = List.copyOf(objs);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ls.size(); i++) {
				if (i == ls.size() - 1 && ls.size() > 1) sb.append(" e ");
				else if (i > 0) sb.append(", ");

				sb.append(ls.get(i));
			}

			return sb.toString();
		};
	}

	public static <C, T extends Collection<C>> String parseAndJoin(T col, Function<C, String> mapper) {
		return col.stream().map(mapper).collect(Collectors.collectingAndThen(Collectors.toList(), properlyJoin()));
	}

	public static String toStringDuration(long millis) {
		long days = millis / MILLIS_IN_DAY;
		millis %= MILLIS_IN_DAY;
		long hours = millis / MILLIS_IN_HOUR;
		millis %= MILLIS_IN_HOUR;
		long minutes = millis / MILLIS_IN_MINUTE;
		millis %= MILLIS_IN_MINUTE;
		long seconds = millis / MILLIS_IN_SECOND;
		seconds %= MILLIS_IN_SECOND;

		return Stream.of(
				days > 0 ? days + " dia" + (days != 1 ? "s" : "") : "",
				hours > 0 ? hours + " hora" + (hours != 1 ? "s" : "") : "",
				minutes > 0 ? minutes + " minuto" + (minutes != 1 ? "s" : "") : "",
				seconds > 0 ? seconds + " segundo" + (seconds != 1 ? "s" : "") : ""
		).filter(s -> !s.isBlank()).collect(Collectors.collectingAndThen(Collectors.toList(), properlyJoin()));
	}

	@SuppressWarnings("unchecked")
	public static <T> T map(Class<T> type, Object[] tuple) {
		try {
			List<Constructor<?>> constructors = Arrays.stream(type.getConstructors())
					.filter(c -> c.getParameterCount() == tuple.length)
					.toList();

			for (Constructor<?> ctor : constructors) {
				try {
					return (T) ctor.newInstance(tuple);
				} catch (IllegalArgumentException ignore) {
				}
			}

			throw new IllegalStateException("No matching constructor found.");
		} catch (Exception e) {
			if (e instanceof InvocationTargetException ex) {
				throw new RuntimeException(ex.getCause());
			}

			throw new RuntimeException(e);
		}
	}

	public static <T> List<T> map(Class<T> type, List<Object[]> records) {
		List<T> result = new LinkedList<>();
		for (Object[] record : records) {
			result.add(map(type, record));
		}
		return result;
	}

	public static List<GifFrame> readGif(String url) throws IOException, ImageReadException {
		ByteSourceInputStream bsis = new ByteSourceInputStream(getImage(url), "temp");

		GifImageParser gip = new GifImageParser();
		GifImageMetadata gim = (GifImageMetadata) gip.getMetadata(bsis);
		List<BufferedImage> frames = gip.getAllBufferedImages(bsis);
		List<GifImageMetadataItem> metas = gim.getItems();
		List<GifFrame> out = new ArrayList<>();

		for (int i = 0; i < Math.min(frames.size(), metas.size()); i++) {
			BufferedImage frame = frames.get(i);
			GifImageMetadataItem meta = metas.get(i);

			out.add(new GifFrame(
							frame,
							meta.getDisposalMethod(),
							gim.getWidth(),
							gim.getHeight(),
							meta.getLeftPosition(),
							meta.getTopPosition(),
							meta.getDelay() * 10
					)
			);
		}

		return out;
	}

	public static List<GifFrame> readGif(String url, boolean uncompress) throws IOException, ImageReadException {
		ByteSourceInputStream bsis = new ByteSourceInputStream(getImage(url), "temp");

		GifImageParser gip = new GifImageParser();
		GifImageMetadata gim = (GifImageMetadata) gip.getMetadata(bsis);
		List<GifImageMetadataItem> metas = gim.getItems();

		List<BufferedImage> frames = gip.getAllBufferedImages(bsis);
		if (uncompress) {
			List<BufferedImage> source = List.copyOf(frames);
			BufferedImage bi = deepCopy(source.get(0));

			BufferedImage finalBi = bi;
			frames = new ArrayList<>() {{
				add(deepCopy(finalBi));
			}};
			Graphics2D g = bi.createGraphics();
			DisposalMethod method = DisposalMethod.UNSPECIFIED;

			for (int i = 1; i < source.size(); i++) {
				GifImageMetadataItem meta = metas.get(i);

				switch (method) {
					case RESTORE_TO_BACKGROUND -> {
						g.dispose();
						bi = deepCopy(source.get(0));
						g = bi.createGraphics();

						BufferedImage frame = source.get(i);
						g.drawImage(frame, 0, 0, null);
						frames.add(deepCopy(bi));
					}
					case RESTORE_TO_PREVIOUS -> {
						g.dispose();
						bi = deepCopy(frames.get(Math.max(0, i - 1)));
						g = bi.createGraphics();

						BufferedImage frame = source.get(i);
						g.drawImage(frame, 0, 0, null);
						frames.add(deepCopy(bi));
					}
					default -> {
						BufferedImage frame = source.get(i);
						g.drawImage(frame, 0, 0, null);
						frames.add(deepCopy(bi));
					}
				}

				method = meta.getDisposalMethod();
			}

			g.dispose();
		}

		List<GifFrame> out = new ArrayList<>();

		for (int i = 0; i < Math.min(frames.size(), metas.size()); i++) {
			BufferedImage frame = frames.get(i);
			GifImageMetadataItem meta = metas.get(i);

			out.add(new GifFrame(
							frame,
							uncompress ? DisposalMethod.RESTORE_TO_BACKGROUND : meta.getDisposalMethod(),
							gim.getWidth(),
							gim.getHeight(),
							meta.getLeftPosition(),
							meta.getTopPosition(),
							meta.getDelay() * 10
					)
			);
		}

		return out;
	}

	public static void makeGIF(File f, List<GifFrame> frames) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						frame.getDelay(),
						0
				);
			}
			gif.finish();
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						frame.getDelay(),
						repeat
				);
			}
			gif.finish();
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat, int delay) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						delay == -1 ? frame.getDelay() : delay,
						repeat
				);
			}
			gif.finish();
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat, int delay, int compress) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						delay == -1 ? frame.getDelay() : delay,
						repeat
				);
			}
			gif.finish();
		}

		if (compress > 0) try {
			Process p = Runtime.getRuntime().exec("mogrify -layers 'optimize' -fuzz " + compress + "% " + f.getAbsolutePath());
			p.waitFor();
		} catch (InterruptedException ignore) {
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat, int delay, int compress, int colors) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						delay == -1 ? frame.getDelay() : delay,
						repeat
				);
			}
			gif.finish();
		}

		if (compress > 0) try {
			Process p = Runtime.getRuntime().exec("mogrify -layers 'optimize' -fuzz " + compress + "% " + f.getAbsolutePath());
			p.waitFor();
		} catch (InterruptedException ignore) {
		}

		if (colors > 0) try {
			Process p = Runtime.getRuntime().exec("gifsicle -O3 --colors " + colors + " --lossy=30 " + f.getAbsolutePath());
			p.waitFor();
		} catch (InterruptedException ignore) {
		}
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	public static byte[] serialize(Object obj) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.flush();
			oos.close();

			return baos.toByteArray();
		}
	}

	public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes); ObjectInputStream bis = new ObjectInputStream(bais)) {
			return bis.readObject();
		}
	}

	public static void stream(InputStream input, OutputStream output) throws IOException {
		byte[] data = new byte[2048];
		int read;
		while ((read = input.read(data)) >= 0) {
			output.write(data, 0, read);
		}

		output.flush();
	}

	public static File compressDir(File file) throws IOException {
		if (file.isDirectory()) {
			Path source = file.toPath();
			File tmp = File.createTempFile("files-all_" + System.currentTimeMillis(), null);
			SevenZOutputFile szof = new SevenZOutputFile(tmp);

			try (szof) {
				Files.walkFileTree(source, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						if (attrs.isSymbolicLink()) return FileVisitResult.CONTINUE;

						Path rel = source.relativize(file);

						try {
							SevenZArchiveEntry entry = szof.createArchiveEntry(file.toFile(), rel.toString());
							szof.putArchiveEntry(entry);
							szof.write(FileUtils.readFileToByteArray(file.toFile()));
							szof.closeArchiveEntry();
						} catch (IOException e) {
							Helper.logger(this.getClass()).error("Error compressing file " + file);
						}

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) {
						Helper.logger(this.getClass()).error("Error opening file " + file);
						return FileVisitResult.CONTINUE;
					}
				});

				szof.finish();

				return tmp;
			}
		}

		return null;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static byte[] compress(File file) throws IOException {
		if (file.isDirectory()) {
			Path source = file.toPath();
			File tmp = File.createTempFile("files-all_" + System.currentTimeMillis(), null);
			SevenZOutputFile szof = new SevenZOutputFile(tmp);

			try (szof) {
				Files.walkFileTree(source, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						if (attrs.isSymbolicLink()) return FileVisitResult.CONTINUE;

						Path rel = source.relativize(file);

						try {
							SevenZArchiveEntry entry = szof.createArchiveEntry(file.toFile(), rel.toString());
							szof.putArchiveEntry(entry);
							szof.write(FileUtils.readFileToByteArray(file.toFile()));
							szof.closeArchiveEntry();
						} catch (IOException e) {
							Helper.logger(this.getClass()).error("Error compressing file " + file);
						}

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) {
						Helper.logger(this.getClass()).error("Error opening file " + file);
						return FileVisitResult.CONTINUE;
					}
				});

				szof.finish();

				try {
					return FileUtils.readFileToByteArray(tmp);
				} finally {
					tmp.delete();
				}
			}
		} else {
			return compress(FileUtils.readFileToByteArray(file));
		}
	}

	public static byte[] compress(String data) throws IOException {
		return compress(data.getBytes(StandardCharsets.UTF_8));
	}

	public static byte[] compress(byte[] bytes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
		GZIPOutputStream gzip = new GZIPOutputStream(baos);

		try (gzip; baos) {
			gzip.write(bytes);
			return baos.toByteArray();
		}
	}

	public static String uncompress(byte[] compressed) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
		GZIPInputStream gis = new GZIPInputStream(bais);
		byte[] bytes = IOUtils.toByteArray(gis);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public static long stringToDurationMillis(String str) {
		Map<String, String> args = extractNamedGroups(str, "(?:(?<DAYS>[0-9]+)d)?(?:(?<HOURS>[0-9]+)h)?(?:(?<MINUTES>[0-9]+)m)?(?:(?<SECONDS>[0-9]+)s)?");
		long out = 0;

		for (Map.Entry<String, String> arg : args.entrySet()) {
			TimeUnit unit = TimeUnit.valueOf(arg.getKey());
			out += unit.toMillis(Integer.parseInt(arg.getValue()));
		}

		return out;
	}

	public static boolean hasAlpha(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if ((image.getRGB(x, y) & 0xFF000000) < 0xFF000000)
					return true;
			}
		}

		return false;
	}

	public static <T> T safeCast(Object obj, Class<T> klass) {
		return klass != null && klass.isInstance(obj) ? klass.cast(obj) : null;
	}

	public static String getUsername(String id) {
		User u = Main.getInfo().getUserByID(id);

		return u == null ? UsernameDAO.getUsername(id) : u.getName();
	}

	public static boolean notNull(Object... objs) {
		return Arrays.stream(objs).allMatch(Objects::nonNull);
	}

	@SafeVarargs
	public static <T> boolean contains(Collection<T> col, T... elem) {
		for (T t : col) {
			if (t.equals(elem)) return true;
		}

		return false;
	}

	public static void drawSquareLine(Graphics2D g2d, int x1, int y1, int x2, int y2) {
		int half = x1 + (x2 - x1) / 2;

		g2d.drawPolyline(
				new int[]{x1, half, half, x2},
				new int[]{y1, y1, y2, y2},
				4
		);
	}

	public static void drawCenteredString(Graphics2D g2d, String str, int x, int y, int width, int height) {
		int xOffset = width / 2 - g2d.getFontMetrics().stringWidth(str) / 2;
		int yOffset = height / 2 + g2d.getFont().getSize() / 2;
		g2d.drawString(str, x + xOffset, y + yOffset);
	}

	public static int findStringInFile(File f, String str) {
		try (Scanner scanner = new Scanner(f, StandardCharsets.UTF_8)) {
			int i = -1;
			while (scanner.hasNextLine()) {
				i++;
				if (scanner.nextLine().equals(str)) {
					return i;
				}
			}
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
		}

		return -1;
	}

	public static int findStringInFile(File f, String str, Function<String, String> mapper) {
		try (Scanner scanner = new Scanner(f, StandardCharsets.UTF_8)) {
			int i = -1;
			while (scanner.hasNextLine()) {
				i++;
				if (mapper.apply(scanner.nextLine()).equals(str)) {
					return i;
				}
			}
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
		}

		return -1;
	}

	public static String getLineFromFile(File f, int line) {
		try (Stream<String> stream = Files.lines(f.toPath(), StandardCharsets.UTF_8)) {
			return stream.skip(line).findFirst().orElse("");
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
		}

		return null;
	}

	public static long getFibonacci(int nth) {
		if (nth <= 1) return nth;

		return getFibonacci(nth - 1) + getFibonacci(nth - 2);
	}

	public static int revFibonacci(int fib) {
		if (fib <= 1) return 2;

		return (int) Helper.log(fib * Math.sqrt(5) + 0.5, Helper.GOLDEN_RATIO);
	}

	public static double getRatio(double w, double h) {
		return w / h;
	}

	public static <T> void replaceContent(Collection<T> from, Collection<T> to) {
		to.removeAll(from);
		to.addAll(from);
	}

	public static <K, V> void replaceContent(Map<K, V> from, Map<K, V> to) {
		for (K key : from.keySet()) {
			from.remove(key);
		}
		to.putAll(from);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Cloneable> T clone(T orig) {
		try {
			return orig != null ? (T) Object.class.getDeclaredMethod("clone").invoke(orig) : null;
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			return orig;
		}
	}

	public static <T> List<T> removeIf(Collection<T> col, Function<T, Boolean> cond) {
		List<T> removed = new ArrayList<>();

		Iterator<T> i = col.iterator();
		while (i.hasNext()) {
			T obj = i.next();

			if (cond.apply(obj)) {
				removed.add(obj);
				i.remove();
			}
		}

		return removed;
	}

	public static <K, V> Map<K, V> removeIf(Map<K, V> map, BiFunction<K, V, Boolean> cond) {
		Map<K, V> removed = new HashMap<>();

		Iterator<Map.Entry<K, V>> i = map.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<K, V> entry = i.next();

			if (cond.apply(entry.getKey(), entry.getValue())) {
				removed.put(entry.getKey(), entry.getValue());
				i.remove();
			}
		}

		return removed;
	}

	public static int toLuma(int r, int g, int b) {
		return (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
	}

	public static int toLuma(int[] rgb) {
		return (int) (0.2126 * rgb[1] + 0.7152 * rgb[2] + 0.0722 * rgb[3]);
	}

	public static int toLuma(int rgb) {
		return (int) (0.2126 * ((rgb >> 16) & 0xFF) + 0.7152 * ((rgb >> 8) & 0xFF) + 0.0722 * (rgb & 0xFF));
	}

	public static Integer[] mergeArrays(Integer[]... arr) {
		List<Integer[]> in = new ArrayList<>(List.of(arr));
		int max = in.stream().map(a -> a.length).max(Integer::compareTo).orElse(0);

		return IntStream.range(0, max)
				.mapToObj(i -> {
					int n = 0;

					for (Integer[] ts : in) {
						if (i < ts.length) n += ts[i];
					}

					return n;
				}).toArray(Integer[]::new);
	}

	public static long stringToLong(String in) {
		String hash = hash(in, "SHA-1");
		return new BigInteger(hash.getBytes(StandardCharsets.UTF_8)).longValue();
	}

	public static String sign(int value) {
		return value > 0 ? "+" + value : String.valueOf(value);
	}

	public static Emoji parseEmoji(String in) {
		if (StringUtils.isNumeric(in)) {
			Emote e = Main.getShiroShards().getEmoteById(in);
			if (e == null) return Emoji.fromMarkdown("❓");

			return Emoji.fromEmote(e);
		}

		return Emoji.fromMarkdown(in);
	}

	public static double getBuffMult(GuildConfig gc, BuffType type) {
		return gc.getBuffs().stream()
				.filter(b -> b.getType() == type)
				.mapToDouble(Buff::getMultiplier)
				.map(d -> d + (gc.isPartner() ? 0.2 : 0))
				.findFirst().orElse(1);
	}

	public static int safeGet(int[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	public static long safeGet(long[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	public static float safeGet(float[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	public static double safeGet(double[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	public static <T> T safeGet(T[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public static <T> T safeGet(List<T> lst, int index) {
		try {
			return lst.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public static <T, O> O castGet(T[] arr, int index, Function<T, O> converter) {
		try {
			return converter.apply(arr[index]);
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static <T, O> O castGet(List<T> lst, int index, Function<T, O> converter) {
		try {
			return converter.apply(lst.get(index));
		} catch (RuntimeException e) {
			return null;
		}
	}
}
