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
import com.kuuhaku.model.common.Extensions;
import com.kuuhaku.model.common.drop.CreditDrop;
import com.kuuhaku.model.common.drop.Prize;
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
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.python.google.common.collect.Lists;

import javax.annotation.Nonnull;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Helper {

	public static final String VOID = "\u200B";
	public static final String CANCEL = "❎";
	public static final String ACCEPT = "✅";
	public static final int CANVAS_SIZE = 1025;
	public static final DateTimeFormatter dateformat = DateTimeFormatter.ofPattern("dd/MMM/yyyy | HH:mm:ss (z)");
	public static final String HOME = "674261700366827539";

	private static PrivilegeLevel getPrivilegeLevel(Member member) {
		if (ShiroInfo.getNiiChan().equals(member.getId()))
			return PrivilegeLevel.NIICHAN;
		else if (ShiroInfo.getDevelopers().contains(member.getId()))
			return PrivilegeLevel.DEV;
		else if (ShiroInfo.getSupports().contains(member.getId()))
			return PrivilegeLevel.SUPPORT;
		else if (member.hasPermission(Permission.MESSAGE_MANAGE))
			return PrivilegeLevel.MOD;
		else if (TagDAO.getTagById(member.getGuild().getOwnerId()).isPartner() || TagDAO.getTagById(member.getId()).isPartner())
			return PrivilegeLevel.PARTNER;
		return PrivilegeLevel.USER;
	}

	public static boolean hasPermission(Member member, PrivilegeLevel privilegeLevel) {
		return getPrivilegeLevel(member).hasAuthority(privilegeLevel);
	}

	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
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

	public static Color colorThief(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		BufferedImage icon = ImageIO.read(con.getInputStream());

		try {
			if (icon != null)
				return new Color(ColorThief.getColor(icon)[0], ColorThief.getColor(icon)[1], ColorThief.getColor(icon)[2]);
			else return getRandomColor();
		} catch (NullPointerException e) {
			return getRandomColor();
		}
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

			if (webhook[0] == null) return chn.createWebhook(name).complete();
			else return webhook[0];
		} catch (InsufficientPermissionException | InterruptedException | ExecutionException e) {
			sendPM(Objects.requireNonNull(chn.getGuild().getOwner()).getUser(), "❌ | " + name + " não possui permissão para criar um webhook em seu servidor no canal " + chn.getAsMention());
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
			EmbedBuilder eb = new EmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(msg);
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
			EmbedBuilder eb = new EmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(msg);
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
		int matches = 0;
		for (String compare : compareWith) {
			matches += Arrays.stream(compareWith).map(String::toLowerCase).allMatch(compare.toLowerCase()::contains) ? 1 : 0;
		}
		return matches == compareWith.length;
	}

	public static boolean containsAny(String[] string, String... compareWith) {
		for (String compare : compareWith) {
			if (Arrays.stream(compareWith).map(String::toLowerCase).allMatch(compare.toLowerCase()::contains))
				return true;
		}
		return false;
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
			if (TagDAO.getTagById(c.getGuild().getOwnerId()).isPartner() && c.getGuild().getMembers().contains(c.getGuild().getMember(Main.getJibril().getSelfUser()))) {
				Member jibril = c.getGuild().getMemberById(Main.getJibril().getSelfUser().getId());
				assert jibril != null;
				EnumSet<Permission> perms = Objects.requireNonNull(c.getGuild().getMemberById(jibril.getId())).getPermissionsExplicit(c);

				jibrilPerms = "\n\n\n__**Permissões atuais da Jibril**__\n\n" +
						perms.stream().map(p -> ":white_check_mark: -> " + p.getName() + "\n").sorted().collect(Collectors.joining());
			}
		} catch (NoResultException ignore) {
		}

		Member shiro = c.getGuild().getSelfMember();
		EnumSet<Permission> perms = shiro.getPermissionsExplicit(c);

		return "__**Permissões atuais da Shiro**__\n\n" +
				perms.stream().map(p -> ":white_check_mark: -> " + p.getName() + "\n").sorted().collect(Collectors.joining()) +
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

	public static void nonPartnerAlert(User author, Member member, MessageChannel channel, String s, String link) {
		try {
			if (!TagDAO.getTagById(author.getId()).isPartner() && !hasPermission(member, PrivilegeLevel.DEV)) {
				channel.sendMessage("❌ | Este comando é exlusivo para parceiros!").queue();
				return;
			}
		} catch (NoResultException e) {
			channel.sendMessage("❌ | Este comando é exlusivo para parceiros!").queue();
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
		JSONObject ja = gc.getButtonConfigs();

		if (ja.isEmpty()) return;

		Guild g = Main.getInfo().getGuildByID(gc.getGuildID());

		ja.keySet().forEach(k -> {
			JSONObject jo = ja.getJSONObject(k);
			Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();

			TextChannel channel = g.getTextChannelById(jo.getString("canalId"));

			if (channel == null) {
				JSONObject newJa = new JSONObject(ja.toString());
				if (k.equals("gatekeeper")) newJa.remove("gatekeeper");
				else newJa.remove(jo.getString("canalId"));
				gc.setButtonConfigs(newJa);
				GuildDAO.updateGuildSettings(gc);
			} else try {
				Message msg = channel.retrieveMessageById(jo.getString("msgId")).submit().get();
				resolveButton(g, jo, buttons);

				if (k.equals("gatekeeper")) {
					buttons.put("\uD83D\uDEAA", (m, v) -> m.kick("Não aceitou as regras.").queue(null, Helper::doNothing));

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

					Pages.buttonize(msg, buttons, true);
				}
			} catch (NullPointerException | ErrorResponseException | InterruptedException | ExecutionException e) {
				JSONObject newJa = new JSONObject(ja.toString());
				if (k.equals("gatekeeper")) newJa.remove("gatekeeper");
				else newJa.remove(jo.getString("msgId"));
				gc.setButtonConfigs(newJa);
				GuildDAO.updateGuildSettings(gc);
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
						g.removeRoleFromMember(m, role).queue();
					} else {
						g.addRoleToMember(m, role).queue();
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
			assert channel != null;
			channel.retrieveMessageById(jo.getString("msgId")).queue(msg -> {
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
		List<Guild> spGuilds = Main.getInfo().getAPI().getGuilds().stream().filter(g -> sponsors.contains(g.getOwnerId()) && g.getSelfMember().hasPermission(Permission.CREATE_INSTANT_INVITE)).collect(Collectors.toList());
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
					sb.append(Helper.createInvite(g).setMaxAge(0).setMaxUses(0).submit().get().getUrl()).append("\n");
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
		InviteAction i = null;
		for (TextChannel tc : guild.getTextChannels()) {
			try {
				i = tc.createInvite().setMaxUses(1);
				break;
			} catch (InsufficientPermissionException | NullPointerException ignore) {
			}
		}
		return i;
	}

	public static String didYouMean(String word, String[] array) {
		String match = "";
		int threshold = 0;

		for (String w : array) {
			if (word.equalsIgnoreCase(w)) {
				return word;
			} else {
				List<Character> firstChars = Lists.charactersOf(word);
				List<Character> secondChars = Lists.charactersOf(w);

				int chars = (int) secondChars.stream().filter(firstChars::contains).count();

				if (chars > threshold) {
					match = w;
					threshold = chars;
				}
			}
		}

		return match;
	}

	public static String replaceEmotes(String msg) {
		String[] args = msg.split(" ");

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(":") && args[i].endsWith(":")) {
				List<Emote> emt = Main.getInfo().getAPI().getEmotesByName(args[i].replace(":", ""), true);
				if (emt.size() > 0) args[i] = emt.get(Helper.rng(emt.size(), true)).getAsMention();
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
		for (int i = 0, slots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(e -> !e.isAnimated()).count(), aSlots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(Emote::isAnimated).count(); i < oldWords.length; i++) {
			if (!oldWords[i].startsWith("&")) {
				newWords[i] = oldWords[i];
				continue;
			}

			boolean makenew = false;
			Emote e;
			try {
				e = g.getEmotesByName(oldWords[i].replace("&", ""), true).get(0);
			} catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
				try {
					e = Main.getInfo().getAPI().getEmotesByName(oldWords[i].replace("&", ""), true).get(0);
					makenew = true;
				} catch (IndexOutOfBoundsException | IllegalArgumentException exc) {
					e = null;
				}
			}

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
		}

		LogDAO.saveLog(new Log().setGuildId(guild.getId()).setGuild(guild.getName()).setUser(author.getAsTag()).setCommand(rawMessage));
		logToChannel(author, true, command, "Um comando foi usado no canal " + ((TextChannel) channel).getAsMention(), guild, rawMessage);
		return false;
	}

	public static float prcnt(float value, float max, int round) {
		return new BigDecimal((value * 100) / max).setScale(round, RoundingMode.HALF_EVEN).floatValue();
	}

	public static int prcntToInt(float value, float max) {
		return Math.round((value * 100) / max);
	}

	public static JSONObject post(String endpoint, JSONObject payload, String token) {
		HttpRequest req = HttpRequest.post(endpoint)
				.header("Content-Type", "application/json; charset=UTF-8")
				.header("Accept", "application/json")
				.header("User-Agent", "Mozilla/5.0")
				.header("Authorization", token)
				.send(payload.toString());

		return new JSONObject(req.body());
	}

	public static JSONObject post(String endpoint, JSONObject payload, Map<String, String> headers, String token) {
		HttpRequest req = HttpRequest.post(endpoint)
				.headers(headers)
				.header("Authorization", token)
				.send(payload.toString());

		return new JSONObject(req.body());
	}

	public static JSONObject post(String endpoint, String payload, Map<String, String> headers, String token) {
		HttpRequest req = HttpRequest.post(endpoint)
				.headers(headers)
				.header("Authorization", token)
				.send(payload);

		return new JSONObject(req.body());
	}

	public static JSONObject get(String endpoint, JSONObject payload, Map<String, String> headers, String token) {
		HttpRequest req = HttpRequest.get(endpoint, payload.toMap(), true)
				.headers(headers)
				.header("Authorization", token);

		return new JSONObject(req.body());
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

	public static void awaitMessage(User u, TextChannel chn, Callable<Boolean> act) {
		Main.getInfo().getAPI().addEventListener(new ListenerAdapter() {
			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getChannel().getId().equals(chn.getId()) && event.getAuthor().getId().equals(u.getId())) {
					try {
						if (act.call()) Main.getInfo().getAPI().removeEventListener(this);
					} catch (Exception ignore) {
					}
				}
			}
		});
	}

	public static <T> List<T> getRandomN(List<T> array, int elements) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();

		for (int i = 0; i <= elements; i++) {
			int index = rng(aux.size(), true);

			out.add(aux.get(index));
			aux.remove(index);
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
		GuildBuff gb = GuildBuffDAO.getBuffs(channel.getGuild().getId());
		ServerBuff cardBuff = gb.getBuffs().stream().filter(b -> b.getId() == 2).findFirst().orElse(null);
		ServerBuff foilBuff = gb.getBuffs().stream().filter(b -> b.getId() == 4).findFirst().orElse(null);
		boolean cbUltimate = cardBuff != null && cardBuff.getTier() == 4;
		boolean fbUltimate = foilBuff != null && foilBuff.getTier() == 4;

		if (cbUltimate || chance(2.5 + (channel.getGuild().getMemberCount() * 1.5 / 5000) * (cardBuff != null ? cardBuff.getMult() : 1))) {
			List<Card> cards = CardDAO.getCards();
			Card c = cards.get(Helper.rng(cards.size(), true));
			boolean foil = fbUltimate || chance(0.5 * (foilBuff != null ? foilBuff.getMult() : 1));
			KawaiponCard kc = new KawaiponCard(c, foil);

			EmbedBuilder eb = new EmbedBuilder();
			eb.setImage("attachment://kawaipon.png");
			eb.setAuthor("Uma carta " + c.getRarity().toString().toUpperCase() + " Kawaipon apareceu neste servidor!");
			eb.setTitle(kc.getName() + " (" + c.getAnime().toString() + ")");
			eb.setColor(getRandomColor());
			eb.setFooter("Digite `" + gc.getPrefix() + "coletar` para adquirir esta carta (necessário: " + (c.getRarity().getIndex() * 300 * (foil ? 2 : 1)) + " créditos).", null);

			try {
				Objects.requireNonNull(channel.getGuild().getTextChannelById(gc.getCanalKawaipon())).sendMessage(eb.build()).addFile(getBytes(c.drawCard(foil), "png"), "kawaipon.png").delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
			} catch (RuntimeException e) {
				gc.setCanalKawaipon(null);
				GuildDAO.updateGuildSettings(gc);
				channel.sendMessage(eb.build()).addFile(getBytes(c.drawCard(foil), "png"), "kawaipon.png").delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
			}
			Main.getInfo().getCurrentCard().put(channel.getGuild().getId(), kc);
		}
	}

	public static void spawnKawaipon(EventChannel channel, TwitchChat chat) {
		if (chance(2.5)) {
			List<Card> cards = CardDAO.getCards();
			Card c = cards.get(Helper.rng(cards.size(), true));
			boolean foil = chance(1);
			KawaiponCard kc = new KawaiponCard(c, foil);

			chat.sendMessage(channel.getName(),
					"FootYellow | " + kc.getName() + " (" + c.getRarity().toString() + " | " + c.getAnime().toString() + ") | Digite \"s!coletar\" para adquirir esta carta (necessário: " + (c.getRarity().getIndex() * 300 * (foil ? 2 : 1)) + " créditos)."
			);
			Main.getInfo().getCurrentCard().put("twitch", kc);
		}
	}

	public static void spawnDrop(GuildConfig gc, TextChannel channel) {
		GuildBuff gb = GuildBuffDAO.getBuffs(channel.getGuild().getId());
		ServerBuff dropBuff = gb.getBuffs().stream().filter(b -> b.getId() == 3).findFirst().orElse(null);
		boolean dbUltimate = dropBuff != null && dropBuff.getTier() == 4;

		if (dbUltimate || chance(2 + (channel.getGuild().getMemberCount() * 1d / 5000) * (dropBuff != null ? dropBuff.getMult() : 1))) {
			Prize drop = new CreditDrop();

			EmbedBuilder eb = new EmbedBuilder();
			eb.setThumbnail("https://i.pinimg.com/originals/86/c0/f4/86c0f4d0f020c3f819a532873ef33704.png");
			eb.setTitle("Um drop apareceu neste servidor!");
			eb.addField("Conteúdo:", drop.getPrize() + " créditos", true);
			eb.addField("Código captcha:", drop.getCaptcha(), true);
			eb.setColor(getRandomColor());
			eb.setFooter("Digite `" + gc.getPrefix() + "abrir` para receber o prêmio (requisitos: " + drop.getRequirement().getKey() + ").", null);

			try {
				Objects.requireNonNull(channel.getGuild().getTextChannelById(gc.getCanalDrop())).sendMessage(eb.build()).delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
			} catch (RuntimeException e) {
				gc.setCanalDrop(null);
				GuildDAO.updateGuildSettings(gc);
				channel.sendMessage(eb.build()).delay(1, TimeUnit.MINUTES).flatMap(Message::delete).queue(null, Helper::doNothing);
			}
			Main.getInfo().getCurrentDrop().put(channel.getGuild().getId(), drop);
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

		// Make sure the aspect ratio is maintained, so the image is not distorted
		double thumbRatio = (double) w / (double) h;
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		double aspectRatio = (double) imageWidth / (double) imageHeight;

		if (thumbRatio > aspectRatio) {
			h = (int) (w / aspectRatio);
		} else {
			w = (int) (h * aspectRatio);
		}

		// Draw the scaled image
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
}
