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
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.coder4.emoji.EmojiUtils;
import com.github.kevinsawicki.http.HttpRequest;
import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.google.gson.Gson;
import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.command.commands.discord.reactions.Reaction;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.GuildBuffDAO;
import com.kuuhaku.controller.postgresql.LogDAO;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.Extensions;
import com.kuuhaku.model.common.drop.CreditDrop;
import com.kuuhaku.model.common.drop.ItemDrop;
import com.kuuhaku.model.common.drop.JokerDrop;
import com.kuuhaku.model.common.drop.Prize;
import com.kuuhaku.model.enums.*;
import com.kuuhaku.model.persistent.*;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Helper {
	public static final String VOID = "\u200B";
	public static final String CANCEL = "❎";
	public static final String ACCEPT = "✅";
	public static final String ANTICOPY = "͏"; //U+034F
	public static final int CANVAS_SIZE = 1025;
	public static final DateTimeFormatter dateformat = DateTimeFormatter.ofPattern("dd/MMM/yyyy | HH:mm:ss (z)");
	public static final String HOME = "674261700366827539";
	public static final int BASE_CARD_PRICE = 350;
	public static final int BASE_EQUIPMENT_PRICE = 500;

	public static Font HAMLIN;

	static {
		try {
			HAMLIN = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Helper.class.getClassLoader().getResourceAsStream("font/Hamlin.ttf")));
		} catch (FontFormatException | IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
		}
	}

	private static PrivilegeLevel getPrivilegeLevel(Member member) {
		if (ShiroInfo.getNiiChan().equals(member.getId()))
			return PrivilegeLevel.NIICHAN;
		else if (ShiroInfo.getDevelopers().contains(member.getId()))
			return PrivilegeLevel.DEV;
		else if (ShiroInfo.getSupports().contains(member.getId()))
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

		return new DecimalFormat("0" + (places > 0 ? "." : "") + StringUtils.repeat("#", places)).format(value);
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

	public static int minMax(int val, int min, int max) {
		return Math.min(Math.max(min, val), max);
	}

	public static long minMax(long val, long min, long max) {
		return Math.min(Math.max(min, val), max);
	}

	public static float minMax(float val, float min, float max) {
		return Math.min(Math.max(min, val), max);
	}

	public static double minMax(double val, double min, double max) {
		return Math.min(Math.max(min, val), max);
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

		final Matcher msg = urlPattern.matcher(text.toLowerCase());
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
		channel.sendTyping().queue(tm -> channel.sendMessage(Helper.makeEmoteFromMention(message.split(" "))).queueAfter(message.length() * 25 > 10000 ? 10000 : message.length() + 500, TimeUnit.MILLISECONDS));
	}

	public static Consumer<MessageAction> sendReaction(Reaction r, String imageURL, User target, MessageChannel channel, boolean allowReact) throws IllegalAccessException {
		try {
			if (r.isAnswerable() && allowReact) {
				return act -> act.queue(m -> Pages.buttonize(m, Collections.singletonMap("↪", (mb, msg) -> {
					if (mb.getId().equals(target.getId())) {
						r.answer((TextChannel) channel);
						msg.clearReactions().queue();
					}
				}), false, 1, TimeUnit.MINUTES, u -> u.getId().equals(target.getId())), Helper::doNothing);
			} else
				return RestAction::queue;
		} catch (Exception e) {
			ShiroInfo.getStaff().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> c.sendMessage("GIF com erro: " + imageURL).queue()));
			logger(Helper.class).error("Erro ao carregar a imagem: " + imageURL + " -> " + e + " | " + e.getStackTrace()[0]);
			throw new IllegalAccessException();
		}
	}

	public static int rng(int maxValue, boolean exclusive) {
		return Math.abs(new Random().nextInt(maxValue + (exclusive ? 0 : 1)));
	}

	public static double rng(double maxValue, boolean exclusive) {
		return Math.abs(new Random().nextDouble() * maxValue + (exclusive ? 0 : 1));
	}

	public static int rng(int maxValue, Random random, boolean exclusive) {
		return Math.abs(random.nextInt(maxValue + (exclusive ? 0 : 1)));
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
		if (Helper.rng(1000, false) > 990) {
			channel.sendMessage("Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\nhttps://top.gg/bot/572413282653306901").queue();
		}
	}

	public static String getAd() {
		if (Helper.rng(1000, false) > 990) {
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

	@SuppressWarnings("rawtypes")
	public static Logger logger(Class source) {
		return LogManager.getLogger(source.getName());
	}

	public static InputStream getImage(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.addRequestProperty("User-Agent", "Mozilla/5.0");
		return con.getInputStream();
	}

	public static Webhook getOrCreateWebhook(TextChannel chn, String name, JDA bot) {
		try {
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
		} catch (InsufficientPermissionException | InterruptedException | ExecutionException ignore) {
			//sendPM(Objects.requireNonNull(chn.getGuild().getOwner()).getUser(), "❌ | " + name + " não possui permissão para criar um webhook em seu servidor no canal " + chn.getAsMention());
		}
		return null;
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
		return String.join(" ", chkdSrc).trim().replace("@everyone", "everyone").replace("@here", "here");
	}

	public static String makeEmoteFromMention(String sourceNoSplit) {
		String[] source = sourceNoSplit.split(" ");
		String[] chkdSrc = new String[source.length];
		for (int i = 0; i < source.length; i++) {
			if (source[i].startsWith("{") && source[i].endsWith("}"))
				chkdSrc[i] = source[i].replace("{", "<").replace("}", ">").replace("&", ":");
			else chkdSrc[i] = source[i];
		}
		return String.join(" ", chkdSrc).trim().replace("@everyone", "everyone").replace("@here", "here");
	}

	public static String stripEmotesAndMentions(String source) {
		return Helper.getOr(StringUtils.normalizeSpace(source.replaceAll("<\\S*>", "")).replace("@everyone", "everyone").replace("@here", "here"), "...");
	}

	public static void logToChannel(User u, boolean isCommand, Command c, String msg, Guild g) {
		GuildConfig gc = GuildDAO.getGuildById(g.getId());
		if (gc.getCanalLog() == null || gc.getCanalLog().isEmpty()) return;
		else if (g.getTextChannelById(gc.getCanalLog()) == null) gc.setCanalLog("");
		try {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(StringUtils.abbreviate(msg, 2048));
			eb.addField("Referente:", u.getAsMention(), true);
			if (isCommand) eb.addField("Comando:", gc.getPrefix() + c.getName(), true);
			eb.setFooter("Data: " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), null);

			Objects.requireNonNull(g.getTextChannelById(gc.getCanalLog())).sendMessage(eb.build()).queue(null, Helper::doNothing);
		} catch (NullPointerException ignore) {
		} catch (Exception e) {
			gc.setCanalLog("");
			GuildDAO.updateGuildSettings(gc);
			logger(Helper.class).warn(e + " | " + e.getStackTrace()[0]);
		}
	}

	public static void logToChannel(User u, boolean isCommand, Command c, String msg, Guild g, String args) {
		GuildConfig gc = GuildDAO.getGuildById(g.getId());
		if (gc.getCanalLog() == null || gc.getCanalLog().isEmpty()) return;
		else if (g.getTextChannelById(gc.getCanalLog()) == null) gc.setCanalLog("");
		try {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(StringUtils.abbreviate(msg, 2048));
			eb.addField("Referente:", u.getAsMention(), true);
			if (isCommand) {
				eb.addField("Comando:", gc.getPrefix() + c.getName(), true);
				eb.addField("Argumentos:", args, true);
			}
			eb.setFooter("Data: " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), null);

			Objects.requireNonNull(g.getTextChannelById(gc.getCanalLog())).sendMessage(eb.build()).queue();
		} catch (NullPointerException ignore) {
		} catch (Exception e) {
			gc.setCanalLog("");
			GuildDAO.updateGuildSettings(gc);
			logger(Helper.class).warn(e + " | " + e.getStackTrace()[0]);
		}
	}

	public static String getRandomHexColor() {
		String[] colorTable = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			sb.append(colorTable[clamp(new Random().nextInt(16), 0, 16)]);
		}
		return "#" + sb.toString();
	}

	public static Color getRandomColor() {
		return new Color(rng(255, false), rng(255, false), rng(255, false));
	}

	public static boolean compareWithValues(int value, int... compareWith) {
		return Arrays.stream(compareWith).anyMatch(v -> v == value);
	}

	public static boolean containsAll(String string, String... compareWith) {
		return Arrays.stream(compareWith).map(String::toLowerCase).allMatch(string.toLowerCase()::contains);
	}

	public static boolean containsAny(String string, String... compareWith) {
		return Arrays.stream(compareWith).map(String::toLowerCase).anyMatch(string.toLowerCase()::contains);
	}

	public static boolean containsAll(String[] string, String... compareWith) {
		return Arrays.stream(string).map(String::toLowerCase).allMatch(s -> ArrayUtils.contains(compareWith, s));
	}

	public static boolean containsAny(String[] string, String... compareWith) {
		return Arrays.stream(string).map(String::toLowerCase).anyMatch(s -> ArrayUtils.contains(compareWith, s));
	}

	public static boolean equalsAll(String string, String... compareWith) {
		return Arrays.stream(compareWith).allMatch(string::equalsIgnoreCase);
	}

	public static boolean equalsAny(String string, String... compareWith) {
		return Arrays.stream(compareWith).anyMatch(string::equalsIgnoreCase);
	}

	public static boolean hasPermission(Member m, Permission p, TextChannel c) {
		boolean allowedPermInChannel = c.getRolePermissionOverrides().stream().anyMatch(po -> m.getRoles().contains(po.getRole()) && po.getAllowed().contains(p)) || c.getMemberPermissionOverrides().stream().anyMatch(po -> po.getMember() == m && po.getAllowed().contains(p));
		boolean deniedPermInChannel = c.getRolePermissionOverrides().stream().anyMatch(po -> m.getRoles().contains(po.getRole()) && po.getDenied().contains(p)) || c.getMemberPermissionOverrides().stream().anyMatch(po -> po.getMember() == m && po.getDenied().contains(p));
		boolean hasPermissionInGuild = m.hasPermission(p);

		return (hasPermissionInGuild && !deniedPermInChannel) || allowedPermInChannel;
	}

	public static String getCurrentPerms(TextChannel c) {
		String jibrilPerms = "";

		try {
			if (TagDAO.getTagById(c.getGuild().getOwnerId()).isBeta() && c.getGuild().getMembers().contains(c.getGuild().getMember(Main.getJibril().getSelfUser()))) {
				Member jibril = c.getGuild().getMemberById(Main.getJibril().getSelfUser().getId());
				assert jibril != null;
				EnumSet<Permission> perms = Objects.requireNonNull(c.getGuild().getMemberById(jibril.getId())).getPermissionsExplicit(c);

				jibrilPerms = "\n\n\n__**Permissões atuais da Jibril**__\n\n" +
							  perms.stream()
									  .map(p -> ":white_check_mark: -> " + p.getName() + "\n")
									  .sorted()
									  .collect(Collectors.joining());
			}
		} catch (NoResultException ignore) {
		}

		Member shiro = c.getGuild().getSelfMember();
		EnumSet<Permission> perms = shiro.getPermissionsExplicit(c);

		return "__**Permissões atuais da Shiro**__\n\n" +
			   perms.stream()
					   .map(p -> ":white_check_mark: -> " + p.getName() + "\n")
					   .sorted()
					   .collect(Collectors.joining()) +
			   jibrilPerms;
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

		if (usrRoles.size() == 0) return false;
		else if (tgtRoles.size() == 0) return true;
		else return usrRoles.get(0).getPosition() > tgtRoles.get(0).getPosition();
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
		List<T> list = new ArrayList<>(set);
		int overflow = list.size() % chunkSize;
		List<List<T>> chunks = new ArrayList<>();

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
		JSONObject btns = gc.getButtonConfigs();

		if (btns.isEmpty()) return;

		Guild g = Main.getInfo().getGuildByID(gc.getGuildID());
		if (g != null)
			source.keySet().forEach(k -> {
				try {
					JSONObject jo = btns.getJSONObject(k);
					Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();

					TextChannel channel = g.getTextChannelById(jo.getString("canalId"));

					if (channel == null) {
						JSONObject newJa = new JSONObject(btns.toString());
						if (k.equals("gatekeeper")) newJa.remove("gatekeeper");
						else newJa.remove(jo.getString("canalId"));
						gc.setButtonConfigs(newJa);
						GuildDAO.updateGuildSettings(gc);
					} else try {
						Message msg = channel.retrieveMessageById(jo.getString("msgId")).submit().get();
						resolveButton(g, jo, buttons);

						if (k.equals("gatekeeper")) {
							buttons.put("\uD83D\uDEAA", (m, v) -> m.kick("Não aceitou as regras.").queue(null, Helper::doNothing));

							msg.clearReactions();
							Pages.buttonize(msg, buttons, false);
						} else {
							buttons.put(CANCEL, (m, ms) -> {
								if (m.getUser().getId().equals(jo.getString("author"))) {
									JSONObject gcjo = gc.getButtonConfigs();
									gcjo.remove(jo.getString("msgId"));
									gc.setButtonConfigs(gcjo);
									GuildDAO.updateGuildSettings(gc);
									ms.clearReactions().queue();
								}
							});

							msg.clearReactions();
							Pages.buttonize(msg, buttons, true);
						}
					} catch (NullPointerException | ErrorResponseException | InterruptedException | ExecutionException e) {
						JSONObject newJa = new JSONObject(btns.toString());
						if (k.equals("gatekeeper")) newJa.remove("gatekeeper");
						else newJa.remove(jo.getString("msgId"));
						gc.setButtonConfigs(newJa);
						GuildDAO.updateGuildSettings(gc);
					}
				} catch (JSONException e) {
					logger(Helper.class).info("Error in buttons JSON: " + source.toString());
				}
			});
	}

	public static void resolveButton(Guild g, JSONObject jo, Map<String, BiConsumer<Member, Message>> buttons) {
		jo.getJSONObject("buttons").keySet().forEach(b -> {
			JSONObject btns = jo.getJSONObject("buttons").getJSONObject(b);
			Role role = g.getRoleById(btns.getString("role"));
			buttons.put(btns.getString("emote"), (m, ms) -> {
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
		});
	}

	public static void gatekeep(GuildConfig gc) {
		JSONObject ja = gc.getButtonConfigs();

		if (ja.isEmpty()) return;

		Guild g = Main.getInfo().getGuildByID(gc.getGuildID());

		ja.keySet().forEach(k -> {
			JSONObject jo = ja.getJSONObject(k);
			Map<String, BiConsumer<Member, Message>> buttons = new HashMap<>();

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
		});
	}

	public static void addButton(String[] args, Message message, MessageChannel channel, GuildConfig gc, String s2, boolean gatekeeper) {
		try {
			JSONObject root = gc.getButtonConfigs();
			String msgId = channel.retrieveMessageById(args[0]).complete().getId();

			JSONObject msg = new JSONObject();

			JSONObject btn = new JSONObject();
			btn.put("emote", EmojiUtils.containsEmoji(s2) ? s2 : Objects.requireNonNull(Main.getInfo().getAPI().getEmoteById(s2)).getId());
			btn.put("role", message.getMentionedRoles().get(0).getId());

			if (!root.has(msgId)) {
				msg.put("msgId", msgId);
				msg.put("canalId", channel.getId());
				msg.put("buttons", new JSONObject());
				msg.put("author", message.getAuthor().getId());
			} else {
				msg = root.getJSONObject(msgId);
			}

			msg.getJSONObject("buttons").put(args[1], btn);

			if (gatekeeper) root.put("gatekeeper", msg);
			else root.put(msgId, msg);

			gc.setButtonConfigs(root);
			GuildDAO.updateGuildSettings(gc);
		} catch (ErrorResponseException e) {
			JSONObject jo = gc.getButtonConfigs();
			if (gatekeeper) jo.remove("gatekeeper");
			else jo.remove(message.getId());
			gc.setButtonConfigs(jo);
			GuildDAO.updateGuildSettings(gc);
		}
	}

	public static String getSponsors() {
		List<String> sponsors = TagDAO.getSponsors().stream().map(Tags::getId).collect(Collectors.toList());
		List<Guild> spGuilds = new ArrayList<>();
		for (String sp : sponsors) {
			spGuilds.add(Main.getInfo().getAPI()
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
			g.retrieveInvites().queue(invs -> invs.forEach(inv -> {
				if (inv.getInviter() == Main.getInfo().getSelfUser()) {
					i.set(inv);
				}
			}));

			if (i.get() == null) {
				try {
					InviteAction ia = Helper.createInvite(g);
					if (ia != null) sb.append(ia.setMaxAge(0).submit().get().getUrl()).append("\n");
				} catch (InterruptedException | ExecutionException e) {
					Helper.logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
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
			try {
				return tc.createInvite();
			} catch (InsufficientPermissionException | NullPointerException ignore) {
			}
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
				int diff = checker.apply(word.toLowerCase(), w.toLowerCase());
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
				Emote e = Main.getInfo().getAPI().getEmoteById(ShiroInfo.getEmoteCache().get(args[i]));
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
				if (g2d.getFontMetrics().stringWidth(sb.toString() + word) > bi.getWidth() - 50) {
					wrappedLines.add(sb.toString().trim());
					sb.setLength(0);
				}
				sb.append(word).append(" ");
			}
			if (sb.length() > 0) wrappedLines.add(sb.toString());
		}
		if (wrappedLines.size() == 0) wrappedLines.add(text);

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

	public static Map<String, Consumer<Void>> sendEmotifiedString(Guild g, String text) {
		String[] oldWords = text.split(" ");
		String[] newWords = new String[oldWords.length];
		List<Consumer<Void>> queue = new ArrayList<>();
		Consumer<Emote> after = e -> e.delete().queue();
		for (int i = 0, emotes = 0, slots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(e -> !e.isAnimated()).count(), aSlots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(Emote::isAnimated).count(); i < oldWords.length && emotes < 10; i++) {
			if (!oldWords[i].startsWith(":") || !oldWords[i].endsWith(":")) {
				newWords[i] = oldWords[i];
				continue;
			}

			boolean makenew = false;
			String id = ShiroInfo.getEmoteCache().get(oldWords[i]);
			Emote e = id == null ? null : Main.getInfo().getAPI().getEmoteById(id);
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
					Helper.logger(Helper.class).error(ex + " | " + ex.getStackTrace()[0]);
				}
				emotes++;
			} else newWords[i] = oldWords[i];
		}

		return Collections.singletonMap(String.join(" ", newWords), aVoid -> queue.forEach(q -> q.accept(null)));
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
			throw t;
		} catch (Throwable ignore) {
		}
	}

	public static boolean showMMError(User author, MessageChannel channel, Guild guild, String rawMessage, Command command) {
		if (author == Main.getInfo().getSelfUser()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_human-command")).queue();
			return true;
		} else if (!hasPermission(guild.getSelfMember(), Permission.MESSAGE_MANAGE, (TextChannel) channel) && GuildDAO.getGuildById(guild.getId()).isServerMMLocked() && command.requiresMM()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-message-manage-permission")).queue();
			return true;
		} else if (!hasPermission(guild.getSelfMember(), Permission.MESSAGE_EMBED_LINKS, (TextChannel) channel)) {
			channel.sendMessage("❌ | A permissão de criar embeds é essencial para que eu funcione, por favor adicione-a ao meu cargo!").queue();
			return true;
		}

		LogDAO.saveLog(new Log().setGuildId(guild.getId()).setGuild(guild.getName()).setUser(author.getAsTag()).setCommand(rawMessage));
		logToChannel(author, true, command, "Um comando foi usado no canal " + ((TextChannel) channel).getAsMention(), guild, rawMessage);
		return false;
	}

	public static float offsetPrcnt(float value, float max, float offset) {
		return Math.max(0, value - offset) / max;
	}

	public static float prcnt(float value, float max) {
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

		return Base64.getEncoder().encodeToString(nameSpace) + "." + Base64.getEncoder().encodeToString(randomSpace);
	}

	public static void awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act) {
		Main.getInfo().getShiroEvents().addHandler(chn.getGuild(), new SimpleMessageListener() {
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

		for (int i = 0; i < elements; i++) {
			int index = rng(aux.size(), true);

			out.add(aux.get(index));
			aux.remove(index);
		}

		return out;
	}

	public static <T> List<T> getRandomN(List<T> array, int elements, int maxInstances) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();

		for (int i = 0; i < elements; i++) {
			int index = rng(aux.size(), true);

			T inst = aux.get(index);
			if (Collections.frequency(out, inst) < maxInstances)
				out.add(inst);
			else i--;
		}

		return out;
	}

	public static <T> List<T> getRandomN(List<T> array, int elements, int maxInstances, long seed) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();
		Random random = new Random(seed);

		for (int i = 0; i < elements; i++) {
			int index = rng(aux.size(), random, true);

			T inst = aux.get(index);
			if (Collections.frequency(out, inst) < maxInstances)
				out.add(inst);
			else i--;
		}

		return out;
	}

	public static String replaceWith(String source, Map<String, String> replaces) {
		AtomicReference<String> toChange = new AtomicReference<>();
		replaces.forEach((k, v) -> toChange.set(source.replace(k, v)));
		return toChange.get();
	}

	public static byte[] getBytes(BufferedImage image) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "jpg", baos);
			return baos.toByteArray();
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			return new byte[0];
		}
	}

	public static byte[] getBytes(BufferedImage image, String encode) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, encode, baos);
			return baos.toByteArray();
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			return new byte[0];
		}
	}

	public static byte[] getBytes(BufferedImage image, String encode, float compression) {
		byte[] bytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageWriter writer = ImageIO.getImageWritersByFormatName(encode).next();
			ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
			writer.setOutput(ios);

			ImageWriteParam param = writer.getDefaultWriteParam();
			if (param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(compression);
			}

			writer.write(null, new IIOImage(image, null, null), param);
			bytes = baos.toByteArray();
		} catch (IOException e) {
			logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
			bytes = new byte[0];
		}

		return bytes;
	}

	public static void spawnKawaipon(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getRatelimit().getIfPresent("kawaipon_" + gc.getGuildID()) != null) return;
		GuildBuff gb = GuildBuffDAO.getBuffs(channel.getGuild().getId());
		ServerBuff cardBuff = gb.getBuffs().stream().filter(b -> b.getId() == 2).findFirst().orElse(null);
		ServerBuff foilBuff = gb.getBuffs().stream().filter(b -> b.getId() == 4).findFirst().orElse(null);
		boolean cbUltimate = cardBuff != null && cardBuff.getTier() == 4;
		boolean fbUltimate = foilBuff != null && foilBuff.getTier() == 4;

		if (cbUltimate || chance((3 - minMax(prcnt(channel.getGuild().getMemberCount(), 5000), 0, 1)) * (cardBuff != null ? cardBuff.getMult() : 1))) {
			KawaiponRarity kr = getRandom(Arrays.stream(KawaiponRarity.validValues())
					.filter(r -> r != KawaiponRarity.ULTIMATE)
					.map(r -> Pair.create(r, (6 - r.getIndex()) / 12d))
					.collect(Collectors.toList())
			);

			List<Card> cards = CardDAO.getCardsByRarity(kr);
			Card c = cards.get(Helper.rng(cards.size(), true));
			boolean foil = fbUltimate || chance(0.5 * (foilBuff != null ? foilBuff.getMult() : 1));
			KawaiponCard kc = new KawaiponCard(c, foil);
			BufferedImage img = c.drawCard(foil);

			EmbedBuilder eb = new EmbedBuilder()
					.setImage("attachment://kawaipon.png")
					.setAuthor("Uma carta " + c.getRarity().toString().toUpperCase() + " Kawaipon apareceu neste servidor!")
					.setTitle(kc.getName() + " (" + c.getAnime().toString() + ")")
					.setColor(colorThief(img))
					.setFooter("Digite `" + gc.getPrefix() + "coletar` para adquirir esta carta (necessário: " + (c.getRarity().getIndex() * BASE_CARD_PRICE * (foil ? 2 : 1)) + " créditos).", null);

			if (gc.getCanalKawaipon() == null || gc.getCanalKawaipon().isEmpty()) {
				channel.sendMessage(eb.build()).addFile(getBytes(img, "png"), "kawaipon.png").delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
			} else {
				TextChannel tc = channel.getGuild().getTextChannelById(gc.getCanalKawaipon());

				if (tc == null) {
					gc.setCanalKawaipon(null);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessage(eb.build()).addFile(getBytes(c.drawCard(foil), "png"), "kawaipon.png").delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
				} else {
					tc.sendMessage(eb.build()).addFile(getBytes(c.drawCard(foil), "png"), "kawaipon.png").delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
				}
			}
			Main.getInfo().getCurrentCard().put(channel.getGuild().getId(), kc);
			Main.getInfo().getRatelimit().put("kawaipon_" + gc.getGuildID(), true);
		}
	}

	public static void forceSpawnKawaipon(GuildConfig gc, Message message, AnimeName anime) {
		TextChannel channel = message.getTextChannel();
		GuildBuff gb = GuildBuffDAO.getBuffs(channel.getGuild().getId());
		ServerBuff foilBuff = gb.getBuffs().stream().filter(b -> b.getId() == 4).findFirst().orElse(null);
		boolean fbUltimate = foilBuff != null && foilBuff.getTier() == 4;
		KawaiponRarity kr;
		List<Card> cards;
		if (anime != null) {
			List<Card> cds = CardDAO.getCardsByAnime(anime);

			kr = getRandom(cds.stream()
					.map(Card::getRarity)
					.filter(r -> r != KawaiponRarity.ULTIMATE)
					.map(r -> Pair.create(r, (7 - r.getIndex()) / 12d))
					.collect(Collectors.toList())
			);

			cards = cds.stream().filter(c -> c.getRarity() == kr).collect(Collectors.toList());
		} else {
			kr = getRandom(Arrays.stream(KawaiponRarity.validValues())
					.filter(r -> r != KawaiponRarity.ULTIMATE)
					.map(r -> Pair.create(r, (7 - r.getIndex()) / 12d))
					.collect(Collectors.toList())
			);

			cards = CardDAO.getCardsByRarity(kr);
		}

		Card c = cards.get(Helper.rng(cards.size(), true));
		boolean foil = fbUltimate || chance(0.5 * (foilBuff != null ? foilBuff.getMult() : 1));
		KawaiponCard kc = new KawaiponCard(c, foil);
		BufferedImage img = c.drawCard(foil);

		EmbedBuilder eb = new EmbedBuilder()
				.setImage("attachment://kawaipon.png")
				.setAuthor(message.getAuthor().getName() + " invocou uma carta " + c.getRarity().toString().toUpperCase() + " neste servidor!")
				.setTitle(kc.getName() + " (" + c.getAnime().toString() + ")")
				.setColor(colorThief(img))
				.setFooter("Digite `" + gc.getPrefix() + "coletar` para adquirir esta carta (necessário: " + (c.getRarity().getIndex() * BASE_CARD_PRICE * (foil ? 2 : 1)) + " créditos).", null);

		if (gc.getCanalKawaipon() == null || gc.getCanalKawaipon().isEmpty()) {
			channel.sendMessage(eb.build()).addFile(getBytes(img, "png"), "kawaipon.png").delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
		} else {
			TextChannel tc = channel.getGuild().getTextChannelById(gc.getCanalKawaipon());

			if (tc == null) {
				gc.setCanalKawaipon(null);
				GuildDAO.updateGuildSettings(gc);
				channel.sendMessage(eb.build()).addFile(getBytes(c.drawCard(foil), "png"), "kawaipon.png").delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
			} else {
				tc.sendMessage(eb.build()).addFile(getBytes(c.drawCard(foil), "png"), "kawaipon.png").delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
			}
		}
		Main.getInfo().getCurrentCard().put(channel.getGuild().getId(), kc);
		Main.getInfo().getRatelimit().put("kawaipon_" + gc.getGuildID(), true);
	}

	public static void spawnKawaipon(EventChannel channel, TwitchChat chat) {
		if (chance(2.5)) {
			KawaiponRarity kr = getRandom(Arrays.stream(KawaiponRarity.validValues())
					.filter(r -> r != KawaiponRarity.ULTIMATE)
					.map(r -> Pair.create(r, (7 - r.getIndex()) / 12d))
					.collect(Collectors.toList())
			);

			List<Card> cards = CardDAO.getCardsByRarity(kr);
			Card c = cards.get(Helper.rng(cards.size(), true));
			boolean foil = chance(1);
			KawaiponCard kc = new KawaiponCard(c, foil);

			chat.sendMessage(channel.getName(),
					"FootYellow | " + kc.getName() + " (" + c.getRarity().toString() + " | " + c.getAnime().toString() + ") | Digite \"s!coletar\" para adquirir esta carta (necessário: " + (c.getRarity().getIndex() * BASE_CARD_PRICE * (foil ? 2 : 1)) + " créditos)."
			);
			Main.getInfo().getCurrentCard().put("twitch", kc);
		}
	}

	public static void spawnDrop(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getRatelimit().getIfPresent("drop_" + gc.getGuildID()) != null) return;
		GuildBuff gb = GuildBuffDAO.getBuffs(channel.getGuild().getId());
		ServerBuff dropBuff = gb.getBuffs().stream().filter(b -> b.getId() == 3).findFirst().orElse(null);
		boolean dbUltimate = dropBuff != null && dropBuff.getTier() == 4;

		if (dbUltimate || chance((2.5 - minMax(prcnt(channel.getGuild().getMemberCount() * 0.75f, 5000), 0, 0.75)) * (dropBuff != null ? dropBuff.getMult() : 1))) {
			int rolled = Helper.rng(100, false);
			Prize drop = rolled > 90 ? new ItemDrop() : rolled > 80 ? new JokerDrop() : new CreditDrop();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setThumbnail("https://i.pinimg.com/originals/86/c0/f4/86c0f4d0f020c3f819a532873ef33704.png")
					.setTitle("Um drop apareceu neste servidor!");
			if (drop instanceof CreditDrop)
				eb.addField("Conteúdo:", drop.getPrize() + " créditos", true);
			else if (drop instanceof JokerDrop)
				eb.addField("Conteúdo:", drop.getPrizeWithPenalty()[0] + "\n__**MAS**__\n" + drop.getPrizeWithPenalty()[1], true);
			else
				eb.addField("Conteúdo:", drop.getPrizeAsItem().getName(), true);
			eb.addField("Código captcha:", drop.getCaptcha(), true)
					.setFooter("Digite `" + gc.getPrefix() + "abrir` para receber o prêmio (requisitos: " + drop.getRequirement().getKey() + ").", null);

			if (gc.getCanalDrop() == null || gc.getCanalDrop().isEmpty()) {
				channel.sendMessage(eb.build()).delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
			} else {
				TextChannel tc = channel.getGuild().getTextChannelById(gc.getCanalDrop());

				if (tc == null) {
					gc.setCanalDrop(null);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessage(eb.build()).delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
				} else {
					tc.sendMessage(eb.build()).delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
				}
			}
			Main.getInfo().getCurrentDrop().put(channel.getGuild().getId(), drop);
			Main.getInfo().getRatelimit().put("drop_" + gc.getGuildID(), true);
		}
	}

	public static void spawnDrop(EventChannel channel, TwitchChat chat) {
		if (chance(2)) {
			Prize drop = new CreditDrop();

			chat.sendMessage(channel.getName(),
					"HolidayPresent | Digite \"s!abrir " + drop.getCaptcha() + "\" para receber o prêmio (" + drop.getPrize() + " créditos | requisitos: " + drop.getRequirement().getKey() + ")."
			);
			Main.getInfo().getCurrentDrop().put("twitch", drop);
		}
	}

	public static boolean chance(double percentage) {
		return Math.random() * 100 < percentage;
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
		graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, w, h, null);

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

	public static List<Triple<Integer, Integer, BufferedImage>> readGIF(String url) throws IOException {
		List<Triple<Integer, Integer, BufferedImage>> frms = new ArrayList<>();
		ImageReader ir = ImageIO.getImageReadersByFormatName("gif").next();
		ImageInputStream iis = ImageIO.createImageInputStream(getImage(url));
		ir.setInput(iis);

		int w = 0;
		int h = 0;
		int frames = ir.getNumImages(true);
		for (int i = 0; i < frames; i++) {
			BufferedImage image = ir.read(i);
			if (i == 0) {
				w = image.getWidth();
				h = image.getHeight();
			}
			JSONObject metadata = new JSONObject(new Gson().toJson(ir.getImageMetadata(i)));

			BufferedImage master = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			master.getGraphics().drawImage(image, metadata.getInt("imageLeftPosition"), metadata.getInt("imageTopPosition"), null);

			frms.add(Triple.of(metadata.getInt("disposalMethod"), metadata.getInt("delayTime"), master));
		}

		return frms;
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
		else if (foilCount == total && normalCount < total) return CardStatus.NORMAL_CARDS;
		else if (normalCount == total && foilCount < total) return CardStatus.FOIL_CARDS;
		else return CardStatus.ALL_CARDS;
	}

	public static void keepMaximumNFiles(File folder, int maximum) {
		if (!folder.isDirectory()) return;
		List<org.apache.commons.lang3.tuple.Pair<File, FileTime>> files = Arrays.stream(folder.listFiles())
				.map(f -> {
					FileTime time;
					try {
						time = Files.getLastModifiedTime(f.toPath());
					} catch (IOException e) {
						time = null;
					}
					return org.apache.commons.lang3.tuple.Pair.of(f, time);
				})
				.collect(Collectors.toList());

		files.removeIf(p -> p.getRight() == null);

		if (files.size() <= maximum) return;

		files.sort(Comparator.comparing(org.apache.commons.lang3.tuple.Pair::getRight));
		while (files.size() > maximum) {
			files.remove(0).getLeft().delete();
		}
	}

	public static String generateHash(Guild guild, User user) {
		return hash((guild.getId() + user.getId() + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8), "SHA-256");
	}

	public static String generateHash(Game game) {
		return hash((game.hashCode() + "" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8), "SHA-256");
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

	public static <T> T getRandom(List<Pair<T, Double>> values) {
		EnumeratedDistribution<T> ed = new EnumeratedDistribution<>(values.stream().sorted(Comparator.comparingDouble(Pair::getValue)).collect(Collectors.toList()));

		return ed.sample();
	}

	public static Kawaipon getDailyDeck() {
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
		long seed = Long.parseLong("" + today.getYear() + today.getMonthValue() + today.getDayOfMonth());
		Kawaipon kp = new Kawaipon();

		kp.setChampions(getRandomN(CardDAO.getAllChampions(), 30, 3, seed));
		kp.setEquipments(getRandomN(CardDAO.getAllEquipments(), 6, 3, seed));
		kp.setFields(getRandomN(CardDAO.getAllFields(), 1, 3, seed));

		return kp;
	}

	public static String toPercent(double value) {
		return (int) (value * 100) + "%";
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

	public static void broadcast(String message, TextChannel channel, User author) {
		Map<String, Boolean> result = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		List<WebhookClient> clients = new ArrayList<>();
		List<GuildConfig> gcs = GuildDAO.getAlertChannels();
		List<List<GuildConfig>> gcPages = Helper.chunkify(gcs, 10);

		for (List<GuildConfig> gs : gcPages) {
			result.clear();
			eb.clear();
			sb.setLength(0);

			for (GuildConfig gc : gs) {
				Guild g = Main.getInfo().getGuildByID(gc.getGuildID());
				if (g == null) continue;
				try {
					TextChannel c = g.getTextChannelById(gc.getCanalAvisos());
					if (c != null && c.canTalk()) {
						Webhook wh = Helper.getOrCreateWebhook(c, "Notificações Shiro", Main.getInfo().getAPI());
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
			result.forEach((key, value) -> sb.append(value ? "+ " : "- ").append(key).append("\n"));
			sb.append("```");

			eb.setTitle("__**STATUS**__ ");
			eb.setDescription(sb.toString());
			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		WebhookMessageBuilder wmb = new WebhookMessageBuilder();
		wmb.setUsername("Stephanie (Notificações Shiro)");
		wmb.setAvatarUrl("https://i.imgur.com/OmiNNMF.png"); //Halloween
		//wmb.setAvatarUrl("https://i.imgur.com/mgA11Rx.png"); //Normal
		wmb.setContent(message.replace("\\n", "\n"));
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
			if (!word.startsWith(":") || !word.endsWith(":")) continue;
			else if (ShiroInfo.getEmoteCache().containsKey(word)) return true;
		}

		return false;
	}
}
