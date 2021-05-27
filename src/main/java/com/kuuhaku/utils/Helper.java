/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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
import com.coder4.emoji.EmojiUtils;
import com.github.kevinsawicki.http.HttpRequest;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingBiConsumer;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.model.common.*;
import com.kuuhaku.model.common.drop.*;
import com.kuuhaku.model.enums.*;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.model.persistent.guild.GuildBuff;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.ServerBuff;
import com.madgag.gif.fmsware.AnimatedGifEncoder;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.gif.DisposalMethod;
import org.apache.commons.imaging.formats.gif.GifImageMetadata;
import org.apache.commons.imaging.formats.gif.GifImageMetadataItem;
import org.apache.commons.imaging.formats.gif.GifImageParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Precision;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Helper {
	public static final String VOID = "\u200B";
	public static final String CANCEL = "❎";
	public static final String ACCEPT = "✅";
	public static final String ANTICOPY = "͏"; //U+034F
	public static final String MENTION = "<@\\d+>|<@!\\d+>";
	public static final int CANVAS_SIZE = 2049;
	public static final DateTimeFormatter fullDateFormat = DateTimeFormatter.ofPattern(I18n.getString("full-date-format"));
	public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(I18n.getString("date-format"));
	public static final String HOME = "674261700366827539";
	public static final int BASE_CARD_PRICE = 400;
	public static final int BASE_EQUIPMENT_PRICE = 2500;
	public static final int BASE_FIELD_PRICE = 50000;
	public static final long MILLIS_IN_DAY = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
	public static final long MILLIS_IN_HOUR = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
	public static final long MILLIS_IN_MINUTE = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
	public static final long MILLIS_IN_SECOND = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

	private static PrivilegeLevel getPrivilegeLevel(Member member) {
		if (member == null)
			return PrivilegeLevel.USER;
		else if (ShiroInfo.getNiiChan().equals(member.getId()))
			return PrivilegeLevel.NIICHAN;
		else if (ShiroInfo.getDevelopers().contains(member.getId()))
			return PrivilegeLevel.DEV;
		else if (ShiroInfo.getSupports().containsKey(member.getId()))
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
				".*?(?:^|[\\W])((ht|f)tp(s?)://|www\\.)(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*?)",
				Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
		text = StringUtils.deleteWhitespace(text);
		text = replaceWith(text, leetSpeak);
		text = (Extensions.checkExtension(text) ? "http://" : "") + text;

		final Matcher msg = urlPattern.matcher(text.toLowerCase(Locale.ROOT));
		return msg.matches();
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
		channel.sendTyping().queue(tm ->
						channel.sendMessage(makeEmoteFromMention(message.split(" ")))
								.queueAfter(message.length() * 25 > 10000 ? 10000 : message.length() + 500, TimeUnit.MILLISECONDS, null, Helper::doNothing)
				, Helper::doNothing);
	}


	public static int rng(int maxValue, boolean exclusive) {
		return Math.abs(new Random().nextInt(maxValue + (exclusive ? 0 : 1)));
	}

	public static double rng(double maxValue) {
		return Math.abs(new Random().nextDouble() * maxValue);
	}

	public static double rng(double maxValue, Random random) {
		return Math.abs(random.nextDouble() * maxValue);
	}

	public static int rng(int maxValue, Random random, boolean exclusive) {
		return Math.abs(random.nextInt(maxValue + (exclusive ? 0 : 1)));
	}

	public static int rng(int maxValue, long seed, boolean exclusive) {
		return Math.abs(new Random(seed).nextInt(maxValue + (exclusive ? 0 : 1)));
	}

	public static Color colorThief(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		BufferedImage icon = ImageIO.read(con.getInputStream());

		try {
			if (icon != null) {
				int[] colors = ColorThief.getColor(icon, 5, false);
				return new Color(colors[0], colors[1], colors[2]);
			} else return getRandomColor();
		} catch (NullPointerException e) {
			return getRandomColor();
		}
	}

	public static Color colorThief(BufferedImage image) {
		int[] colors = ColorThief.getColor(image, 5, false);
		return new Color(colors[0], colors[1], colors[2]);
	}

	public static void spawnAd(MessageChannel channel) {
		if (rng(1000, false) > 990) {
			channel.sendMessage("Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\nhttps://top.gg/bot/572413282653306901").queue();
		}
	}

	public static String getAd() {
		if (rng(1000, false) > 990) {
			return "Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\nhttps://top.gg/bot/572413282653306901";
		} else return null;
	}

	public static JSONObject callApi(String url) {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			con.addRequestProperty("User-Agent", "Mozilla/5.0");
			return new JSONObject(IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}

	public static Logger logger(Class<?> source) {
		return LogManager.getLogger(source.getName());
	}

	public static InputStream getImage(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.addRequestProperty("User-Agent", "Mozilla/5.0");
		return con.getInputStream();
	}

	public static Webhook getOrCreateWebhook(TextChannel chn, String name, JDA bot) throws InterruptedException, ExecutionException {
		final Webhook[] webhook = {null};
		List<Webhook> whs = chn.retrieveWebhooks().submit().get();
		whs.stream()
				.filter(w -> Objects.requireNonNull(w.getOwner()).getUser() == bot.getSelfUser())
				.findFirst()
				.ifPresent(w -> webhook[0] = w);

		try {
			if (webhook[0] == null) return chn.createWebhook(name).complete();
			else {
				webhook[0].getUrl();
				return webhook[0];
			}
		} catch (NullPointerException e) {
			return chn.createWebhook(name).complete();
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
				return chn.createWebhook(name).complete();
			else {
				return webhook.get();
			}
		} catch (NullPointerException e) {
			return chn.createWebhook(name).complete();
		}
	}

	public static Color reverseColor(Color c) {
		float[] hsv = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsv);
		hsv[0] = (hsv[0] * 360 + 180) / 360;

		return Color.getHSBColor(hsv[0], hsv[1], hsv[2]);
	}

	public static String makeEmoteFromMention(String[] source) {
		String[] chkdSrc = new String[source.length];
		for (int i = 0; i < source.length; i++) {
			if (source[i].startsWith("{") && source[i].endsWith("}"))
				chkdSrc[i] = source[i].replace("{", "<").replace("}", ">").replace("&", ":");
			else chkdSrc[i] = source[i];
		}
		return String.join(" ", chkdSrc).trim().replace("@everyone", bugText("@everyone")).replace("@here", bugText("@here"));
	}

	public static String makeEmoteFromMention(String sourceNoSplit) {
		String[] source = sourceNoSplit.split(" ");
		String[] chkdSrc = new String[source.length];
		for (int i = 0; i < source.length; i++) {
			if (source[i].startsWith("{") && source[i].endsWith("}"))
				chkdSrc[i] = source[i].replace("{", "<").replace("}", ">").replace("&", ":");
			else chkdSrc[i] = source[i];
		}
		return String.join(" ", chkdSrc).trim().replace("@everyone", bugText("@everyone")).replace("@here", bugText("@here"));
	}

	public static String stripEmotesAndMentions(String source) {
		return getOr(StringUtils.normalizeSpace(source.replaceAll("<\\S*>", "")).replace("@everyone", bugText("@everyone")).replace("@here", bugText("@here")), "...");
	}

	public static void logToChannel(User u, boolean isCommand, PreparedCommand c, String msg, Guild g) {
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
			if (u != null) eb.addField("Referente:", u.getAsMention(), true);
			if (isCommand) eb.addField("Comando:", gc.getPrefix() + c.getName(), true);
			eb.setTimestamp(Instant.now());

			tc.sendMessage(eb.build()).queue(null, Helper::doNothing);
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

			tc.sendMessage(eb.build()).queue();
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
			sb.append(colorTable[clamp(new Random().nextInt(16), 0, 16)]);
		}
		return "#" + sb;
	}

	public static Color getRandomColor() {
		return Color.decode("#%06x".formatted(rng(0xFFFFFF, false)));
	}

	public static Color getRandomColor(long seed) {
		return Color.decode("#%06x".formatted(rng(0xFFFFFF, seed, false)));
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

	public static boolean containsAny(String[] string, String... compareWith) {
		return Arrays.stream(string).map(String::toLowerCase).anyMatch(s -> ArrayUtils.contains(compareWith, s));
	}

	@SafeVarargs
	public static <T> boolean containsAny(T[] value, T... compareWith) {
		return Arrays.stream(value).anyMatch(s -> ArrayUtils.contains(compareWith, s));
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

	public static <T> T getOr(T get, T or) {
		try {
			return get == null || (get instanceof String && ((String) get).isBlank()) ? or : get;
		} catch (Exception e) {
			return or;
		}
	}

	public static boolean hasRoleHigherThan(Member user, Member target) {
		List<Role> usrRoles = user.getRoles();
		List<Role> tgtRoles = target.getRoles();

		if (usrRoles.isEmpty()) return false;
		else if (tgtRoles.isEmpty()) return true;
		else return usrRoles.get(0).getPosition() >= tgtRoles.get(0).getPosition();
	}

	public static <T> List<List<T>> chunkify(Collection<T> col, int chunkSize) {
		List<T> list = List.copyOf(col);
		int overflow = list.size() % chunkSize;
		List<List<T>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

		chunks.add(list.subList(list.size() - overflow, list.size()));

		return chunks;
	}

	public static <T> List<List<T>> chunkify(List<T> list, int chunkSize) {
		int overflow = list.size() % chunkSize;
		List<List<T>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

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

		author.openPrivateChannel().queue(c -> c.sendMessage(eb.build()).queue());
	}

	public static void finishEmbed(Guild guild, List<Page> pages, List<MessageEmbed.Field> f, EmbedBuilder eb, int i) {
		eb.setColor(getRandomColor());
		eb.setAuthor("Para usar estes emotes, utilize o comando \"" + GuildDAO.getGuildById(guild.getId()).getPrefix() + "say MENÇÃO\"");
		eb.setFooter("Página " + (i + 1) + ". Mostrando " + (-10 + 10 * (i + 1)) + " - " + (Math.min(10 * (i + 1), f.size())) + " resultados.", null);

		pages.add(new Page(PageType.EMBED, eb.build()));
	}

	public static void refreshButtons(GuildConfig gc) {
		JSONObject source = gc.getButtonConfigs();

		if (source.isEmpty()) return;

		Guild g = Main.getInfo().getGuildByID(gc.getGuildId());
		if (g != null) {
			for (String k : source.keySet()) {
				try {
					JSONObject jo = source.getJSONObject(k);
					Map<String, ThrowingBiConsumer<Member, Message>> buttons = new LinkedHashMap<>();

					TextChannel channel = g.getTextChannelById(jo.getString("canalId"));

					if (channel == null) {
						JSONObject newJa = new JSONObject(source.toString());
						if (k.equals("gatekeeper")) {
							newJa.put("gatekeeper", "");
							newJa.remove("gatekeeper");
						} else {
							newJa.put(jo.getString("canalId"), "");
							newJa.remove(jo.getString("canalId"));
						}
						gc.setButtonConfigs(newJa);
						GuildDAO.updateGuildSettings(gc);
					} else try {
						Message msg = channel.retrieveMessageById(jo.getString("msgId")).submit().get();
						resolveButton(g, jo, buttons);

						if (k.equals("gatekeeper")) {
							buttons.put("\uD83D\uDEAA", (m, v) -> m.kick("Não aceitou as regras.").queue(null, Helper::doNothing));

							msg.clearReactions().queue();
							Pages.buttonize(msg, buttons, false);
						} else {
							buttons.put(CANCEL, (m, ms) -> {
								if (m.getUser().getId().equals(jo.getString("author"))) {
									JSONObject gcjo = gc.getButtonConfigs();
									gcjo.put(jo.getString("msgId"), "");
									gcjo.remove(jo.getString("msgId"));
									gc.setButtonConfigs(gcjo);
									GuildDAO.updateGuildSettings(gc);
									ms.clearReactions().queue();
								}
							});

							msg.clearReactions().queue();
							Pages.buttonize(msg, buttons, true);
						}
					} catch (RuntimeException | InterruptedException | ExecutionException e) {
						JSONObject newJa = new JSONObject(source.toString());
						if (k.equals("gatekeeper")) {
							newJa.put("gatekeeper", "");
							newJa.remove("gatekeeper");
						} else {
							newJa.put(jo.getString("canalId"), "");
							newJa.remove(jo.getString("canalId"));
						}
						gc.setButtonConfigs(newJa);
						GuildDAO.updateGuildSettings(gc);
					}
				} catch (JSONException e) {
					logger(Helper.class).error("Error in buttons JSON: " + source + "\nReason: " + e.getMessage());
					gc.setButtonConfigs(new JSONObject());
					GuildDAO.updateGuildSettings(gc);
				}
			}
		}
	}

	public static void resolveButton(Guild g, JSONObject jo, Map<String, ThrowingBiConsumer<Member, Message>> buttons) {
		JSONObject btns = jo.getJSONObject("buttons");
		Set<String> keys = btns.keySet();
		List<String> sortedKeys = new ArrayList<>();

		for (String k : keys) {
			sortedKeys.add(btns.optInt("index", 0), k);
		}

		for (String b : sortedKeys) {
			JSONObject btn = btns.getJSONObject(b);
			Role role = g.getRoleById(btn.getString("role"));

			buttons.put(btn.getString("emote"), (m, ms) -> {
				if (role != null) {
					if (m.getRoles().contains(role)) {
						g.removeRoleFromMember(m, role).queue(null, Helper::doNothing);
					} else {
						g.addRoleToMember(m, role).queue(null, Helper::doNothing);
					}
				} else {
					ms.clearReactions().queue(s -> {
						ms.getChannel().sendMessage(":warning: | Botões removidos devido a cargo inexistente.").queue();
						GuildConfig gc = GuildDAO.getGuildById(g.getId());
						JSONObject bt = jo.getJSONObject("buttons");
						bt.remove(b);
						jo.put("buttons", bt);
						gc.setButtonConfigs(jo);
						GuildDAO.updateGuildSettings(gc);
					});
				}
			});
		}
	}

	public static void gatekeep(GuildConfig gc) {
		JSONObject ja = gc.getButtonConfigs();

		if (ja.isEmpty()) return;

		Guild g = Main.getInfo().getGuildByID(gc.getGuildId());

		for (String k : ja.keySet()) {
			JSONObject jo = ja.getJSONObject(k);
			Map<String, ThrowingBiConsumer<Member, Message>> buttons = new HashMap<>();

			TextChannel channel = g.getTextChannelById(jo.getString("canalId"));
			if (channel != null) channel.retrieveMessageById(jo.getString("msgId")).queue(msg -> {
				resolveButton(g, jo, buttons);

				buttons.put("\uD83D\uDEAA", (m, v) -> {
					try {
						m.kick("Não aceitou as regras.").queue();
					} catch (InsufficientPermissionException ignore) {
					}
				});

				Pages.buttonize(msg, buttons, false);
			}, Helper::doNothing);
		}
	}

	public static void addButton(String[] args, Message message, MessageChannel channel, GuildConfig gc, String s2, boolean gatekeeper) {
		try {
			JSONObject root = gc.getButtonConfigs();
			String msgId = channel.retrieveMessageById(args[0]).complete().getId();

			String id;
			if (EmojiUtils.containsEmoji(s2)) id = s2;
			else {
				Emote e = Main.getShiroShards().getEmoteById(s2);
				if (e == null) throw new IllegalArgumentException();
				else id = e.getId();
			}

			JSONObject msg = new JSONObject();
			JSONObject btn = new JSONObject() {{
				put("emote", id);
				put("role", message.getMentionedRoles().get(0).getId());
			}};

			if (!root.has(msgId)) {
				msg.put("msgId", msgId);
				msg.put("canalId", channel.getId());
				msg.put("buttons", new JSONObject());
				msg.put("author", message.getAuthor().getId());
			} else {
				msg = root.getJSONObject(msgId);
			}

			btn.put("index", msg.getJSONObject("buttons").length());
			msg.getJSONObject("buttons").put(args[1], btn);

			if (gatekeeper) root.put("gatekeeper", msg);
			else root.put(msgId, msg);

			gc.setButtonConfigs(root);
			GuildDAO.updateGuildSettings(gc);
		} catch (ErrorResponseException | IllegalArgumentException e) {
			JSONObject jo = gc.getButtonConfigs();
			if (gatekeeper) jo.remove("gatekeeper");
			else jo.remove(message.getId());
			gc.setButtonConfigs(jo);
			GuildDAO.updateGuildSettings(gc);
		}
	}

	public static String getSponsors() {
		List<String> sponsors = TagDAO.getSponsors().stream().map(Tags::getUid).collect(Collectors.toList());
		List<Guild> spGuilds = new ArrayList<>();
		for (String sp : sponsors) {
			spGuilds.add(Main.getShiroShards()
					.getGuilds()
					.stream()
					.filter(g -> g.getOwnerId().equals(sp) && g.getSelfMember().hasPermission(Permission.CREATE_INSTANT_INVITE))
					.max(Comparator.comparing(Guild::getMemberCount))
					.orElse(null));
		}

		spGuilds.removeIf(Objects::isNull);
		StringBuilder sb = new StringBuilder();

		for (Guild g : spGuilds) {
			AtomicReference<Invite> i = new AtomicReference<>();
			g.retrieveInvites().queue(invs -> {
				for (Invite inv : invs) {
					if (inv.getInviter() == Main.getSelfUser()) {
						i.set(inv);
					}
				}
			});

			if (i.get() == null) {
				try {
					InviteAction ia = createInvite(g);
					if (ia != null) sb.append(ia.setMaxAge(0).submit().get().getUrl()).append("\n");
				} catch (InterruptedException | ExecutionException e) {
					logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
				}
			} else {
				sb.append(i.get().getUrl()).append("\n");
			}
		}

		return sb.toString();
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
		if (guild.getDefaultChannel() != null) {
			return guild.getDefaultChannel().createInvite();
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
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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

	public static Map<String, Runnable> sendEmotifiedString(Guild g, String text) {
		String[] oldLines = text.split("\n");
		String[] newLines = new String[oldLines.length];
		List<Consumer<Void>> queue = new ArrayList<>();
		Consumer<Emote> after = e -> e.delete().queue();

		for (int l = 0; l < oldLines.length; l++) {
			String[] oldWords = oldLines[l].split(" ");
			String[] newWords = new String[oldWords.length];
			for (int i = 0, emotes = 0, slots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(e -> !e.isAnimated()).count(), aSlots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(Emote::isAnimated).count(); i < oldWords.length && emotes < 10; i++) {
				if (!oldWords[i].startsWith(":") || !oldWords[i].endsWith(":")) {
					newWords[i] = oldWords[i];
					continue;
				}

				boolean makenew = false;
				String id = ShiroInfo.getEmoteLookup().get(oldWords[i]);
				Emote e = id == null ? null : Main.getShiroShards().getEmoteById(id);
				if (e != null && !Objects.requireNonNull(e.getGuild()).getId().equals(g.getId()))
					makenew = true;

				if (e != null) {
					try {
						boolean animated = e.isAnimated();
						if (makenew && (animated ? aSlots : slots) > 0) {
							e = g.createEmote(e.getName(), Icon.from(getImage(e.getImageUrl())), g.getSelfMember().getRoles().get(0)).complete();
							Emote finalE = e;
							queue.add(aVoid -> after.accept(finalE));
							if (animated) aSlots--;
							else slots--;
						}
						newWords[i] = e.getAsMention();
					} catch (IOException ex) {
						logger(Helper.class).error(ex + " | " + ex.getStackTrace()[0]);
					}
					emotes++;
				} else newWords[i] = oldWords[i];
			}

			newLines[l] = String.join(" ", newWords);
		}

		return Collections.singletonMap(String.join("\n", newLines), () -> {
			for (Consumer<Void> q : queue) {
				q.accept(null);
			}
		});
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
		return Math.round((value * 100) / max);
	}

	public static JSONObject post(String endpoint, JSONObject payload, String token) {
		try {
			HttpRequest req = HttpRequest.post(endpoint)
					.header("Content-Type", "application/json; charset=UTF-8")
					.header("Accept", "application/json")
					.header("User-Agent", "Mozilla/5.0")
					.header("Authorization", token)
					.send(payload.toString());

			return new JSONObject(req.body());
		} catch (JSONException e) {
			return new JSONObject();
		}
	}

	public static JSONObject post(String endpoint, JSONObject payload, Map<String, String> headers) {
		try {
			HttpRequest req = HttpRequest.post(endpoint)
					.headers(headers)
					.send(payload.toString());

			return new JSONObject(req.body());
		} catch (JSONException e) {
			return new JSONObject();
		}
	}

	public static JSONObject post(String endpoint, JSONObject payload, Map<String, String> headers, String token) {
		try {
			HttpRequest req = HttpRequest.post(endpoint)
					.headers(headers)
					.header("Authorization", token)
					.send(payload.toString());

			return new JSONObject(req.body());
		} catch (JSONException e) {
			return new JSONObject();
		}
	}

	public static JSONObject post(String endpoint, String payload, Map<String, String> headers, String token) {
		try {
			HttpRequest req = HttpRequest.post(endpoint)
					.headers(headers)
					.header("Authorization", token)
					.send(payload);

			return new JSONObject(req.body());
		} catch (JSONException e) {
			return new JSONObject();
		}
	}

	public static JSONObject get(String endpoint, JSONObject payload, String token) {
		try {
			HttpRequest req = HttpRequest.get(endpoint, payload.toMap(), true)
					.header("Content-Type", "application/json; charset=UTF-8")
					.header("Accept", "application/json")
					.header("User-Agent", "Mozilla/5.0")
					.header("Authorization", token);

			return new JSONObject(req.body());
		} catch (JSONException e) {
			return new JSONObject();
		}
	}

	public static JSONObject get(String endpoint, JSONObject payload, Map<String, String> headers, String token) {
		try {
			HttpRequest req = HttpRequest.get(endpoint, payload.toMap(), true)
					.headers(headers)
					.header("Authorization", token);

			return new JSONObject(req.body());
		} catch (JSONException e) {
			return new JSONObject();
		}
	}

	public static String urlEncode(JSONObject payload) {
		String[] params = payload.toMap().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);

		return String.join("&", params);
	}

	public static String generateToken(String seed, int length) {
		SecureRandom sr = new SecureRandom();
		byte[] nameSpace = seed.getBytes(StandardCharsets.UTF_8);
		byte[] randomSpace = new byte[length];
		sr.nextBytes(randomSpace);

		return atob(nameSpace) + "." + atob(randomSpace);
	}

	public static void awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act) {
		ShiroInfo.getShiroEvents().addHandler(chn.getGuild(), new SimpleMessageListener() {
			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getChannel().getId().equals(chn.getId()) && event.getAuthor().getId().equals(u.getId())) {
					if (act.apply(event.getMessage()))
						close();
				}
			}
		});
	}

	public static <T> List<T> getRandomN(List<T> array, int elements) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();
		Random random = new Random(System.currentTimeMillis());

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = rng(aux.size(), random, true);

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
			int index = rng(aux.size(), random, true);

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
			int index = rng(aux.size(), random, true);

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
		GuildBuff gb = GuildBuffDAO.getBuffs(channel.getGuild().getId());
		ServerBuff cardBuff = gb.getBuffs().stream()
				.filter(b -> b.getType() == BuffType.CARD)
				.findFirst().orElse(null);
		ServerBuff foilBuff = gb.getBuffs().stream()
				.filter(b -> b.getType() == BuffType.FOIL)
				.findFirst().orElse(null);
		boolean cbUltimate = cardBuff != null && cardBuff.getTier() == 4;
		boolean fbUltimate = foilBuff != null && foilBuff.getTier() == 4;

		if (cbUltimate || chance((3 - clamp(prcnt(channel.getGuild().getMemberCount(), 5000), 0, 1)) * (cardBuff != null ? cardBuff.getMult() : 1))) {
			KawaiponRarity kr = getRandom(Arrays.stream(KawaiponRarity.validValues())
					.filter(r -> r != KawaiponRarity.ULTIMATE)
					.map(r -> org.apache.commons.math3.util.Pair.create(r, (15 - r.getIndex()) / 60d))
					.collect(Collectors.toList())
			);

			List<Card> cards = CardDAO.getCardsByRarity(kr);
			Card c = cards.get(rng(cards.size(), true));
			boolean foil = fbUltimate || chance(0.5 * (foilBuff != null ? foilBuff.getMult() : 1));
			KawaiponCard kc = new KawaiponCard(c, foil);
			BufferedImage img = c.drawCard(foil);

			EmbedBuilder eb = new EmbedBuilder()
					.setAuthor("Uma carta " + c.getRarity().toString().toUpperCase(Locale.ROOT) + " Kawaipon apareceu neste servidor!")
					.setTitle(kc.getName() + " (" + c.getAnime().toString() + ")")
					.setColor(colorThief(img))
					.setFooter("Digite `" + gc.getPrefix() + "coletar` para adquirir esta carta (necessário: " + separate(c.getRarity().getIndex() * BASE_CARD_PRICE * (foil ? 2 : 1)) + " créditos).", null);

			if (gc.isSmallCards())
				eb.setThumbnail("attachment://kawaipon.png");
			else
				eb.setImage("attachment://kawaipon.png");

			if (gc.getKawaiponChannel() == null) {
				channel.sendMessage(eb.build()).addFile(writeAndGet(img, "kp_" + c.getId(), "png"), "kawaipon.png")
						.delay(1, TimeUnit.MINUTES)
						.flatMap(Message::delete)
						.queue(null, Helper::doNothing);
			} else {
				TextChannel tc = gc.getKawaiponChannel();

				if (tc == null) {
					gc.setKawaiponChannel(null);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessage(eb.build()).addFile(writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, Helper::doNothing);
				} else {
					tc.sendMessage(eb.build()).addFile(writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, Helper::doNothing);
				}
			}
			Main.getInfo().getCurrentCard().put(channel.getGuild().getId(), kc);
			Main.getInfo().getRatelimit().put("kawaipon_" + gc.getGuildId(), true);
		}
	}

	public static void forceSpawnKawaipon(GuildConfig gc, TextChannel channel, User user, AddedAnime anime, boolean foil) {
		GuildBuff gb = GuildBuffDAO.getBuffs(channel.getGuild().getId());
		ServerBuff foilBuff = gb.getBuffs().stream()
				.filter(b -> b.getType() == BuffType.FOIL)
				.findFirst().orElse(null);
		boolean fbUltimate = foilBuff != null && foilBuff.getTier() == 4;
		KawaiponRarity kr;
		List<Card> cards;
		if (anime != null) {
			List<Card> cds = CardDAO.getCardsByAnime(anime.getName());

			kr = getRandom(cds.stream()
					.map(Card::getRarity)
					.filter(r -> r != KawaiponRarity.ULTIMATE)
					.map(r -> org.apache.commons.math3.util.Pair.create(r, (15 - r.getIndex()) / 60d))
					.collect(Collectors.toList())
			);

			cards = cds.stream().filter(c -> c.getRarity() == kr).collect(Collectors.toList());
		} else {
			kr = getRandom(Arrays.stream(KawaiponRarity.validValues())
					.filter(r -> r != KawaiponRarity.ULTIMATE)
					.map(r -> org.apache.commons.math3.util.Pair.create(r, (15 - r.getIndex()) / 60d))
					.collect(Collectors.toList())
			);

			cards = CardDAO.getCardsByRarity(kr);
		}

		Card c = cards.get(rng(cards.size(), true));
		foil = foil || fbUltimate || chance(0.5 * (foilBuff != null ? foilBuff.getMult() : 1));
		KawaiponCard kc = new KawaiponCard(c, foil);
		BufferedImage img = c.drawCard(foil);

		EmbedBuilder eb = new EmbedBuilder()
				.setAuthor(user.getName() + " invocou uma carta " + c.getRarity().toString().toUpperCase(Locale.ROOT) + " neste servidor!")
				.setTitle(kc.getName() + " (" + c.getAnime().toString() + ")")
				.setColor(colorThief(img))
				.setFooter("Digite `" + gc.getPrefix() + "coletar` para adquirir esta carta (necessário: " + separate(c.getRarity().getIndex() * BASE_CARD_PRICE * (foil ? 2 : 1)) + " créditos).", null);

		if (gc.isSmallCards())
			eb.setThumbnail("attachment://kawaipon.png");
		else
			eb.setImage("attachment://kawaipon.png");

		if (gc.getKawaiponChannel() == null) {
			channel.sendMessage(eb.build()).addFile(writeAndGet(img, "kp_" + c.getId(), "png"), "kawaipon.png")
					.delay(1, TimeUnit.MINUTES)
					.flatMap(Message::delete)
					.queue(null, Helper::doNothing);
		} else {
			TextChannel tc = gc.getKawaiponChannel();

			if (tc == null) {
				gc.setKawaiponChannel(null);
				GuildDAO.updateGuildSettings(gc);
				channel.sendMessage(eb.build()).addFile(writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
						.delay(1, TimeUnit.MINUTES)
						.flatMap(Message::delete)
						.queue(null, Helper::doNothing);
			} else {
				tc.sendMessage(eb.build()).addFile(writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
						.delay(1, TimeUnit.MINUTES)
						.flatMap(Message::delete)
						.queue(null, Helper::doNothing);
			}
		}
		Main.getInfo().getCurrentCard().put(channel.getGuild().getId(), kc);
		Main.getInfo().getRatelimit().put("kawaipon_" + gc.getGuildId(), true);
	}

	public static void spawnDrop(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getRatelimit().containsKey("drop_" + gc.getGuildId())) return;
		GuildBuff gb = GuildBuffDAO.getBuffs(channel.getGuild().getId());
		ServerBuff dropBuff = gb.getBuffs().stream()
				.filter(b -> b.getType() == BuffType.DROP)
				.findFirst().orElse(null);
		boolean dbUltimate = dropBuff != null && dropBuff.getTier() == 4;

		if (dbUltimate || chance((2.5 - clamp(prcnt(channel.getGuild().getMemberCount() * 0.75f, 5000), 0, 0.75)) * (dropBuff != null ? dropBuff.getMult() : 1))) {
			Prize<?> drop;
			int type = rng(1000, false);

			if (type >= 995)
				drop = new FieldDrop();
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

			if (gc.getDropChannel() == null) {
				channel.sendMessage(eb.build()).delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
			} else {
				TextChannel tc = gc.getDropChannel();

				if (tc == null) {
					gc.setDropChannel(null);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessage(eb.build()).delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
				} else {
					tc.sendMessage(eb.build()).delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
				}
			}
			Main.getInfo().getCurrentDrop().put(channel.getGuild().getId(), drop);
			Main.getInfo().getRatelimit().put("drop_" + gc.getGuildId(), true);
		}
	}

	public static void spawnPadoru(GuildConfig gc, TextChannel channel) {
		String padoru = ShiroInfo.RESOURCES_URL + "/assets/padoru_padoru.gif";
		if (Main.getInfo().getSpecialEvent().containsKey(gc.getGuildId())) return;

		if (chance(0.1 - clamp(prcnt(channel.getGuild().getMemberCount() * 0.09, 5000), 0, 0.09))) {
			try {
				TextChannel tc = getOr(gc.getDropChannel(), channel);
				Webhook wh = getOrCreateWebhook(tc, "Shiro");
				WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();

				WebhookMessageBuilder wmb = new WebhookMessageBuilder();
				wmb.setUsername("Nero (Evento Padoru)");
				wmb.setAvatarUrl(ShiroInfo.NERO_AVATAR.formatted(1));

				List<Prize<?>> prizes = new ArrayList<>();
				for (int i = 0; i < 6; i++) {
					int type = rng(1000, false);

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
				SimpleMessageListener sml = new SimpleMessageListener() {
					@Override
					public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
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
						User u = Main.getInfo().getUserByID(ids.get(rng(ids.size(), true)));

						Account acc = AccountDAO.getAccount(u.getId());

						ColorlessWebhookEmbedBuilder neb = new ColorlessWebhookEmbedBuilder()
								.setImageUrl(padoru);

						for (int i = 0; i < prizes.size(); i++) {
							Prize<?> prize = prizes.get(i);
							neb.addField("Presente " + (i + 1) + ":", prize.toString(u), true);
							prize.award(u);
						}

						AccountDAO.saveAccount(acc);
						wc.send(wmb.setContent("Decidi que " + u.getAsMention() + " merece os presentes!")
								.addEmbeds(neb.build())
								.build());
					} else {
						wc.send(wmb.setContent("Decidi que ninguém merece os presentes!").build());
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

				Main.getInfo().getSpecialEvent().put(gc.getGuildId(), true);
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException ignore) {
			}
		}
	}

	public static void spawnUsaTan(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getSpecialEvent().containsKey(gc.getGuildId())) return;

		if (chance(0.15 - clamp(prcnt(channel.getGuild().getMemberCount() * 0.5, 5000), 0, 0.05))) {
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
					int type = rng(1000, false);

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

				List<Message> hist = tc.getHistory()
						.retrievePast(100)
						.complete();

				if (hist.size() == 0) return;

				Message m = hist.get(rng(hist.size(), true));
				Pages.buttonize(m, Collections.singletonMap(
						egg.getId(), (mb, ms) -> {
							if (finished.get()) return;

							ColorlessWebhookEmbedBuilder neb = new ColorlessWebhookEmbedBuilder();
							for (int i = 0; i < prizes.size(); i++) {
								Prize<?> prize = prizes.get(i);
								neb.addField("Ovo " + (i + 1) + ":", prize.toString(mb.getUser()), true);
								prize.award(mb.getUser());
							}

							wc.send(wmb.resetEmbeds()
									.setContent(mb.getAsMention() + " encontrou o ovo de páscoa!")
									.addEmbeds(neb.build())
									.build());
							wc.close();
							found.set(true);
							finished.set(true);
						}
				), false, 2, TimeUnit.MINUTES);

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

				Main.getInfo().getSpecialEvent().put(gc.getGuildId(), true);
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException ignore) {
			}
		}
	}

	public static boolean chance(double percentage) {
		return Math.round(Math.random() * 100) < percentage;
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

		BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = newImage.createGraphics();
		graphics2D.drawImage(image, 0, 0, w, h, null);

		return newImage;
	}

	public static BufferedImage scaleAndCenterImage(BufferedImage image, int w, int h) {
		image = scaleImage(image, w, h);

		int offX = Math.min((image.getWidth() - w) / -2, 0);
		int offY = Math.min((image.getHeight() - h) / -2, 0);

		BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = newImage.createGraphics();
		graphics2D.drawImage(image, offX, offY, null);

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

	public static String getFileType(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("HEAD");
		con.addRequestProperty("User-Agent", "Mozilla/5.0");
		con.connect();
		return con.getContentType();
	}

	public static String hash(byte[] bytes, String encoding) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance(encoding).digest(bytes));
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
		int normalCount = (int) kp.getCards().stream().filter(cd -> !cd.isFoil()).count();
		int foilCount = (int) kp.getCards().stream().filter(KawaiponCard::isFoil).count();
		int total = (int) CardDAO.totalCards();

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
		Matcher m = Pattern.compile("\\{.*}").matcher(text);
		List<MatchResult> results = m.results().collect(Collectors.toList());

		for (MatchResult mr : results) {
			try {
				return new JSONObject(mr.group());
			} catch (JSONException ignore) {
			}
		}

		return null;
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
		dk.setEquipments(getRandomN(CardDAO.getAllEquipments(), 6, 3, seed));
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

	public static boolean regex(String text, @Language("RegExp") String regex) {
		return Pattern.compile(regex).matcher(text).matches();
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
		String[] out;
		Matcher m = Pattern.compile(regex).matcher(text);
		if (m.find()) out = new String[m.groupCount()];
		else out = new String[0];

		if (out.length > 1) out[0] = m.group();
		for (int i = 1; i < out.length; i++) {
			out[i] = m.group(i);
		}

		return List.of(out);
	}

	public static String extract(String text, @Language("RegExp") String regex, String group) {
		Matcher m = Pattern.compile(regex).matcher(text);
		if (m.find()) return m.group(group);
		else return null;
	}

	public static void broadcast(String message, TextChannel channel, User author) {
		Map<String, Boolean> result = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		List<WebhookClient> clients = new ArrayList<>();
		List<GuildConfig> gcs = GuildDAO.getAlertChannels();
		List<List<GuildConfig>> gcPages = chunkify(gcs, 10);

		for (List<GuildConfig> gs : gcPages) {
			result.clear();
			eb.clear();
			sb.setLength(0);

			for (GuildConfig gc : gs) {
				Guild g = Main.getInfo().getGuildByID(gc.getGuildId());
				if (g == null) continue;
				try {
					TextChannel c = gc.getAlertChannel();
					if (c != null && c.canTalk()) {
						Webhook wh = getOrCreateWebhook(c, "Notificações Shiro");
						if (wh == null) result.put(g.getName(), false);
						else {
							WebhookClientBuilder wcb = new WebhookClientBuilder(wh.getUrl());
							clients.add(wcb.build());
							result.put(g.getName(), true);
						}
					} else result.put(g.getName(), false);
				} catch (Exception e) {
					result.put(g.getName(), false);
				}
			}

			sb.append("```diff\n");
			for (Map.Entry<String, Boolean> entry : result.entrySet()) {
				String key = entry.getKey();
				Boolean value = entry.getValue();
				sb.append(value ? "+ " : "- ").append(key).append("\n");
			}
			sb.append("```");

			eb.setTitle("__**STATUS**__ ");
			eb.setDescription(sb.toString());
			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		WebhookMessageBuilder wmb = new WebhookMessageBuilder();
		wmb.setUsername("Stephanie (Notificações Shiro)");

		int v = 1; //1 = Normal | 2 = Halloween
		wmb.setAvatarUrl(ShiroInfo.STEPHANIE_AVATAR.formatted(v)); //Normal
		wmb.setContent(message);
		WebhookCluster cluster = new WebhookCluster(clients);
		cluster.broadcast(wmb.build());
		if (channel != null)
			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
			);
	}

	public static void applyMask(BufferedImage source, BufferedImage mask, int channel) {
		BufferedImage newMask = new BufferedImage(source.getWidth(), source.getHeight(), mask.getType());
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

	public static String getFancyNumber(int number, boolean animated) {
		Map<Character, String> emotes = Map.of(
				'0', "<:0_n:795486513541939230>",
				'1', "<:1_n:795486513618092042>",
				'2', "<:2_n:795486513412046908>",
				'3', "<:3_n:795486513319772211>",
				'4', "<:4_n:795486513197744178>",
				'5', "<:5_n:795486513235492875>",
				'6', "<:6_n:795486513328554008>",
				'7', "<:7_n:795486513067720755>",
				'8', "<:8_n:795486513428824075>",
				'9', "<:9_n:795486513143742465>"
		);

		String sNumber = String.valueOf(number);
		StringBuilder sb = new StringBuilder();
		for (char c : sNumber.toCharArray())
			sb.append(emotes.get(c));

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
		try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(b64))) {
			return ImageIO.read(bais);
		} catch (IOException | NullPointerException e) {
			return null;
		}
	}

	public static String separate(Object value) {
		try {
			Number n = value instanceof Number ? (Number) value : NumberUtils.createNumber(String.valueOf(value));
			DecimalFormat df = new DecimalFormat();
			df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));
			df.setGroupingSize(3);

			return df.format(n);
		} catch (NumberFormatException e) {
			return String.valueOf(value);
		}
	}

	public static String replaceTags(String text, User author, Guild guild) {
		return text.replace("%user%", author.getName())
				.replace("%guild%", guild.getName())
				.replace("%count%", String.valueOf(guild.getMemberCount()));
	}

	public static boolean isPureMention(String msg) {
		return regex(msg, "<(@|@!)\\d+>");
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

	public static MatchInfo mergeInfo(List<MatchInfo> infos) {
		MatchInfo mi = new MatchInfo("");

		for (MatchInfo info : infos) {
			for (Map.Entry<String, Integer> entry : info.getInfo().entrySet()) {
				mi.getInfo().merge(entry.getKey(), entry.getValue(), Helper::average);
			}
		}

		return mi;
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
		String hash = hash((System.currentTimeMillis() + hash(bytes, "MD5")).getBytes(StandardCharsets.UTF_8), "MD5");
		File f = new File(Main.getInfo().getTemporaryFolder(), hash);

		try {
			f.createNewFile();
			FileUtils.writeByteArrayToFile(f, bytes);
			return "https://api." + System.getenv("SERVER_URL") + "/image?id=" + hash;
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
			return "https://api." + System.getenv("SERVER_URL") + "/image?id=" + hash;
		} catch (IOException e) {
			return null;
		}
	}

	public static int applyTax(String id, int raw, double tax) {
		boolean victorious = ExceedDAO.hasExceed(id) && Main.getInfo().getWinner().equals(ExceedDAO.getExceed(id));

		return raw - (victorious ? 0 : (int) (raw * tax));
	}

	public static <T> MessageAction generateStore(User u, TextChannel chn, String title, String desc, List<T> items, Function<T, MessageEmbed.Field> fieldExtractor) {
		Account acc = AccountDAO.getAccount(u.getId());
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(title)
				.setDescription(desc)
				.setFooter("""
						:coin: Créditos: %s
						:diamonds: Gemas: %s
						""".formatted(acc.getBalance(), acc.getGems()));

		for (T item : items) {
			eb.addField(fieldExtractor.apply(item));
		}

		return chn.sendMessage(eb.build());
	}

	public static <T> MessageAction generateStore(User u, TextChannel chn, String title, String desc, Color color, List<T> items, Function<T, MessageEmbed.Field> fieldExtractor) {
		Account acc = AccountDAO.getAccount(u.getId());
		EmbedBuilder eb = new EmbedBuilder()
				.setTitle(title)
				.setDescription(desc)
				.setColor(color)
				.setFooter("""
						💰 Créditos: %s
						💎 Gemas: %s
						""".formatted(separate(acc.getBalance()), separate(acc.getGems())));

		for (T item : items) {
			eb.addField(fieldExtractor.apply(item));
		}

		return chn.sendMessage(eb.build());
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

	public static double[] normalize(double[] vector) {
		double length = Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2));
		return new double[]{vector[0] / length, vector[1] / length};
	}

	public static float[] normalize(float[] vector) {
		double length = Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2));
		return new float[]{(float) (vector[0] / length), (float) (vector[1] / length)};
	}

	public static int[] normalize(int[] vector, RoundingMode roundingMode) {
		double length = Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2));
		return switch (roundingMode) {
			case UP, CEILING, HALF_UP -> new int[]{(int) mirroredCeil(vector[0] / length), (int) mirroredCeil(vector[1] / length)};
			case DOWN, FLOOR, HALF_DOWN -> new int[]{(int) mirroredFloor(vector[0] / length), (int) mirroredFloor(vector[1] / length)};
			case HALF_EVEN -> new int[]{(int) Math.round(vector[0] / length), (int) Math.round(vector[1] / length)};
			default -> throw new IllegalArgumentException();
		};
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
		byte[] bytes = Main.getInfo().getResourceCache().computeIfAbsent(path, s -> {
			InputStream is = klass.getClassLoader().getResourceAsStream(path);

			if (is == null) return new byte[0];
			else {
				try {
					return Helper.getBytes(ImageIO.read(is), path.split("\\.")[1]);
				} catch (IOException e) {
					return new byte[0];
				}
			}
		});

		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
			return ImageIO.read(bais);
		} catch (IOException e) {
			return null;
		}
	}

	public static int roundTrunc(int value, int mult) {
		return mult * (Math.round((float) value / mult));
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

	public static void applyOverlay(BufferedImage in, BufferedImage overlay) {
		Graphics2D g2d = in.createGraphics();
		g2d.drawImage(overlay, 0, 0, null);
		g2d.dispose();
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
		if (m.getAttachments().size() > 0) {
			Message.Attachment att = m.getAttachments().get(0);
			if (att.isImage())
				return att.getUrl();
		} else if (m.getEmbeds().size() > 0) {
			MessageEmbed emb = m.getEmbeds().get(0);
			if (emb.getImage() != null)
				return emb.getImage().getUrl();
		} else if (m.getEmotes().size() > 0) {
			Emote e = m.getEmotes().stream().findFirst().orElse(null);
			if (e != null)
				return m.getEmotes().get(0).getImageUrl();
		}

		return null;
	}

	public static Function<List<String>, String> properlyJoin() {
		return objs -> {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < objs.size(); i++) {
				if (i == objs.size() - 1 && objs.size() > 1) sb.append(" e ");
				else if (i > 0) sb.append(", ");

				sb.append(objs.get(i));
			}

			return sb.toString();
		};
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

		return List.of(
				days > 0 ? days + " dia" + (days != 1 ? "s" : "") : "",
				hours > 0 ? hours + " hora" + (hours != 1 ? "s" : "") : "",
				minutes > 0 ? minutes + " minuto" + (minutes != 1 ? "s" : "") : "",
				seconds > 0 ? seconds + " segundo" + (seconds != 1 ? "s" : "") : ""
		).stream().filter(s -> !s.isBlank()).collect(Collectors.collectingAndThen(Collectors.toList(), properlyJoin()));
	}

	public static <T> T map(Class<T> type, Object[] tuple) {
		List<Class<?>> tupleTypes = new ArrayList<>();
		for (Object field : tuple) {
			tupleTypes.add(field.getClass());
		}
		try {
			Constructor<T> ctor = type.getConstructor(tupleTypes.toArray(new Class<?>[tuple.length]));
			return ctor.newInstance(tuple);
		} catch (Exception e) {
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
					case UNSPECIFIED, DO_NOT_DISPOSE -> {
						BufferedImage frame = source.get(i);
						g.drawImage(frame, 0, 0, null);
						frames.add(Helper.deepCopy(bi));
					}
					case RESTORE_TO_BACKGROUND -> {
						g.dispose();
						bi = deepCopy(source.get(0));
						g = bi.createGraphics();

						BufferedImage frame = source.get(i);
						g.drawImage(frame, 0, 0, null);
						frames.add(Helper.deepCopy(bi));
					}
					case RESTORE_TO_PREVIOUS -> {
						g.dispose();
						bi = deepCopy(frames.get(Math.max(0, i - 1)));
						g = bi.createGraphics();

						BufferedImage frame = source.get(i);
						g.drawImage(frame, 0, 0, null);
						frames.add(Helper.deepCopy(bi));
					}
					default -> frames.add(Helper.deepCopy(bi));
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
		try (FileOutputStream fos = new FileOutputStream(f)) {
			AnimatedGifEncoder gif = new AnimatedGifEncoder();
			gif.setRepeat(0);
			gif.start(fos);
			for (GifFrame frame : frames) {
				gif.setDispose(frame.getDisposal().ordinal());
				gif.setDelay(frame.getDelay());
				gif.addFrame(frame.getAdjustedFrame());
			}
			gif.finish();
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(f)) {
			AnimatedGifEncoder gif = new AnimatedGifEncoder();
			gif.setRepeat(repeat);
			gif.start(fos);
			for (GifFrame frame : frames) {
				gif.setBackground(Color.magenta);
				gif.setTransparent(Color.magenta);
				gif.setDispose(frame.getDisposal().ordinal());
				gif.setDelay(frame.getDelay());
				gif.addFrame(frame.getAdjustedFrame());
			}
			gif.finish();
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat, int delay) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(f)) {
			AnimatedGifEncoder gif = new AnimatedGifEncoder();
			gif.setRepeat(repeat);
			gif.start(fos);
			for (GifFrame frame : frames) {
				gif.setBackground(Color.magenta);
				gif.setTransparent(Color.magenta);
				gif.setDispose(frame.getDisposal().ordinal());
				gif.setDelay(delay);
				gif.addFrame(frame.getAdjustedFrame());
			}
			gif.finish();
		}
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
}
