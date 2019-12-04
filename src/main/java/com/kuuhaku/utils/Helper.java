/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.events.MessageListener;
import com.kuuhaku.model.Extensions;
import com.kuuhaku.model.GamblePool;
import com.kuuhaku.model.guildConfig;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

	public static final String VOID = "\u200B";
	private static final String PREVIOUS = "\u25C0";
	private static final String CANCEL = "\u274E";
	private static final String NEXT = "\u25B6";
	public static final String ACCEPT = "\u2705";
	public static final int CANVAS_SIZE = 1024;
	public static final DateTimeFormatter dateformat = DateTimeFormatter.ofPattern("dd/MMM/yyyy | HH:mm:ss (z)");


	private static PrivilegeLevel getPrivilegeLevel(Member member) {
		if (Main.getInfo().getNiiChan().contains(member.getUser().getId())) {
			return PrivilegeLevel.NIICHAN;
		} else if (Main.getInfo().getDevelopers().contains(member.getUser().getId())) {
			return PrivilegeLevel.DEV;
		} else if (Main.getInfo().getSheriffs().contains(member.getUser().getId())) {
			return PrivilegeLevel.SHERIFF;
		} else if (member.hasPermission(Permission.MESSAGE_MANAGE)) {
			return PrivilegeLevel.MOD;
		} else if (member.getRoles().stream().anyMatch(r -> StringUtils.containsIgnoreCase(r.getName(), "dj"))) {
			return PrivilegeLevel.DJ;
		}
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

	public static int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(val, max));
	}

	public static boolean findURL(String text) {
		final Pattern urlPattern = Pattern.compile(
				".*?(?:^|[\\W])((ht|f)tp(s?)://|www\\.)(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*?)",
				Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
		text = (Extensions.checkExtension(text) ? "http://" : "") + text;
		text = text.replace("1", "i").replace("!", "i");
		text = text.replace("3", "e");
		text = text.replace("4", "a");
		text = text.replace("5", "s");
		text = text.replace("7", "t");
		text = text.replace("0", "o");
		text = text.replace(" ", "");
		text = text.replace("#", ".").replace("%", ".").replace("$", ".").replace("@", ".").replace("*", ".").replace("#", ".").replace("&", ".");

		final Matcher msg = urlPattern.matcher(text.toLowerCase());
		return msg.matches();
	}

	public static boolean findMentions(String text) {
		final Pattern everyone = Message.MentionType.EVERYONE.getPattern();
		final Pattern here = Message.MentionType.HERE.getPattern();

		return everyone.matcher(text).matches() || here.matcher(text).matches();
	}

	public static void sendPM(User user, String message) {
		user.openPrivateChannel().queue((channel) -> channel.sendMessage(message).queue());
	}

	public static void typeMessage(MessageChannel channel, String message) {
		channel.sendTyping().queue(tm -> channel.sendMessage(Helper.makeEmoteFromMention(message.split(" "))).queueAfter(message.length() * 25 > 10000 ? 10000 : message.length() + 500, TimeUnit.MILLISECONDS));
	}

	public static void sendReaction(String imageURL, MessageChannel channel, String message, boolean reacted) {
		channel.sendMessage("Conectando à API...").addFile(new File(Objects.requireNonNull(Helper.class.getClassLoader().getResource("loading.gif")).getPath())).queue(msg -> {
			try {
				if (ImageIO.read(getImage(imageURL)).getWidth() >= 400) {
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(imageURL);
					if (reacted)
						channel.sendMessage(message).embed(eb.build()).queue(m -> m.addReaction("\u21aa").queue());
					else channel.sendMessage(message).embed(eb.build()).queue();
				} else {
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(imageURL);
					if (reacted)
						channel.sendMessage(message + "\n:warning: | GIF com proporções irregulares, os desenvolvedores já foram informados.").embed(eb.build()).queue(m -> m.addReaction("\u21aa").queue());
					else
						channel.sendMessage(message + "\n:warning: | GIF com proporções irregulares, os desenvolvedores já foram informados.").embed(eb.build()).queue();
					logger(Helper.class).warn("GIF irregular: " + imageURL);
					Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> c.sendMessage("GIF irregular: " + imageURL).queue()));
				}
			} catch (Exception e) {
				logger(Helper.class).error("Erro ao carregar a imagem: " + e.getStackTrace()[0]);
			} finally {
				msg.delete().queue();
			}
		});
	}

	public static int rng(int maxValue) {
		return Math.abs(new Random().nextInt(maxValue));
	}

	public static Color colorThief(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		BufferedImage icon = ImageIO.read(con.getInputStream());

		return new Color(ColorThief.getColor(icon)[0], ColorThief.getColor(icon)[1], ColorThief.getColor(icon)[2]);
	}

	public static List<String> getGamble() {
		GamblePool gp = new GamblePool();
		String[] icon = {":cheese:", ":izakaya_lantern:", ":moneybag:", ":diamond_shape_with_a_dot_inside:", ":rosette:", "<a:Wow:598497560734203926>"};
		for (int i = 0; i < icon.length; i++) {
			gp.addGamble(new GamblePool.Gamble(icon[i], icon.length - i));
		}
		String[] pool = gp.getPool();
		List<String> result = new ArrayList<>();
		result.add(pool[clamp(rng(pool.length), 0, pool.length - 1)]);
		result.add(pool[clamp(rng(pool.length), 0, pool.length - 1)]);
		result.add(pool[clamp(rng(pool.length), 0, pool.length - 1)]);
		result.add(pool[clamp(rng(pool.length), 0, pool.length - 1)]);
		result.add(pool[clamp(rng(pool.length), 0, pool.length - 1)]);
		result.add(pool[clamp(rng(pool.length), 0, pool.length - 1)]);
		return result;
	}

	public static void spawnAd(MessageChannel channel) {
		if (Helper.rng(1000) > 990) {
			channel.sendMessage("Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\nhttps://discordbots.org/bot/572413282653306901").queue();
		}
	}

	public static Logger logger(Class source) {
		return LogManager.getLogger(source.getName());
	}

	public static InputStream getImage(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.addRequestProperty("User-Agent", "Mozilla/5.0");
		return con.getInputStream();
	}

	public static Webhook getOrCreateWebhook(TextChannel chn) {
		try {
			final Webhook[] webhook = {null};
			chn.retrieveWebhooks().queue(whs -> whs.stream().filter(w -> Objects.requireNonNull(w.getOwner()).getUser() == Main.getJibril().getSelfUser()).findFirst().ifPresent(webhook1 -> webhook[0] = webhook1));
			if (webhook[0] == null) return chn.createWebhook("Jibril").complete();
			else return webhook[0];
		} catch (InsufficientPermissionException e) {
			sendPM(Objects.requireNonNull(chn.getGuild().getOwner()).getUser(), ":x: | A Jibril não possui permissão para criar um webhook em seu servidor");
		}
		return null;
	}

	public static Color reverseColor(Color c) {
		float[] hsv = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsv);
		hsv[2] = (hsv[2] + 180) % 360;

		return Color.getHSBColor(hsv[0], hsv[1], hsv[2]);
	}

	public static String makeEmoteFromMention(String[] source) {
		String[] chkdSrc = new String[source.length];
		for (int i = 0; i < source.length; i++) {
			if (source[i].startsWith("{") && source[i].endsWith("}"))
				chkdSrc[i] = source[i].replace("{", "<").replace("}", ">").replace("&", ":");
			else chkdSrc[i] = source[i];
		}
		return String.join(" ", chkdSrc).trim();
	}

	public static void logToChannel(User u, boolean isCommand, Command c, String msg, Guild g) {
		guildConfig gc = SQLite.getGuildById(g.getId());
		if (gc.getLogChannel() == null || gc.getLogChannel().isEmpty()) return;
		else if (g.getTextChannelById(gc.getLogChannel()) == null) gc.setLogChannel("");
		try {
			EmbedBuilder eb = new EmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(msg);
			eb.addField("Referente:", u.getAsMention(), true);
			if (isCommand) eb.addField("Comando:", gc.getPrefix() + c.getName(), true);
			eb.setFooter("Data: " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), null);

			Objects.requireNonNull(g.getTextChannelById(gc.getLogChannel())).sendMessage(eb.build()).queue();
		} catch (NullPointerException ignore) {
		} catch (Exception e) {
			gc.setLogChannel("");
			SQLite.updateGuildSettings(gc);
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
		return new Color(rng(255), rng(255), rng(255));
	}

	public static boolean compareWithValues(int value, int... compareWith) {
		return Arrays.stream(compareWith).anyMatch(v -> v == value);
	}

	public static boolean containsAll(String string, String... compareWith) {
		return Arrays.stream(compareWith).allMatch(string::contains);
	}

	public static void paginate(Message msg, List<MessageEmbed> pages) {
		try {
			msg.addReaction(PREVIOUS).queue();
			msg.addReaction(CANCEL).queue();
			msg.addReaction(NEXT).queue();
			Main.getInfo().getAPI().addEventListener(new MessageListener() {
				private final int maxP = pages.size() - 1;
				private int p = 0;
				private Future<?> timeout;
				private final Consumer<Void> success = s -> Main.getInfo().getAPI().removeEventListener(this);

				@Override
				public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
					if (timeout == null) timeout = msg.clearReactions().queueAfter(60, TimeUnit.SECONDS, success);
					if (event.getUser().isBot()) return;

					timeout.cancel(true);
					timeout = msg.clearReactions().queueAfter(60, TimeUnit.SECONDS, success);
					if (event.getReactionEmote().getName().equals(PREVIOUS)) {
						if (p > 0) {
							p--;
							msg.editMessage(pages.get(p)).queue();
						}
					} else if (event.getReactionEmote().getName().equals(NEXT)) {
						if (p < maxP) {
							p++;
							msg.editMessage(pages.get(p)).queue();
						}
					} else if (event.getReactionEmote().getName().equals(CANCEL)) {
						msg.clearReactions().queue(success);
					}
				}

				@Override
				public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
					if (event.getMessageId().equals(msg.getId())) {
						timeout.cancel(true);
						timeout = null;
					}
				}
			});
		} catch (Exception e) {
			logger(Helper.class).warn(e + " | " + e.getStackTrace()[0]);
		}
	}

	public static void categorize(Message msg, Map<String, MessageEmbed> categories) {
		try {
			categories.keySet().forEach(k -> msg.addReaction(k).queue());
			msg.addReaction(CANCEL).queue();
			Main.getInfo().getAPI().addEventListener(new MessageListener() {
				private String currCat = "";
				private Future<?> timeout;
				private final Consumer<Void> success = s -> Main.getInfo().getAPI().removeEventListener(this);

				@Override
				public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
					if (timeout == null) timeout = msg.clearReactions().queueAfter(60, TimeUnit.SECONDS, success);

					if (event.getUser().isBot() || event.getReactionEmote().getName().equals(currCat)) return;
					else if (event.getReactionEmote().getName().equals(CANCEL)) {
						msg.clearReactions().queue(s -> Main.getInfo().getAPI().removeEventListener(this));
						return;
					}

					timeout.cancel(true);
					timeout = msg.clearReactions().queueAfter(60, TimeUnit.SECONDS, success);
					msg.editMessage(categories.get(event.getReactionEmote().getName())).queue(s -> currCat = event.getReactionEmote().getName());
				}

				@Override
				public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
					if (event.getMessageId().equals(msg.getId())) {
						timeout.cancel(true);
						timeout = null;
					}
				}
			});
		} catch (Exception e) {
			logger(Helper.class).warn(e + " | " + e.getStackTrace()[0]);
		}
	}

	public static String getRequiredPerms(TextChannel c) {
		EnumSet channelPerms = c.getGuild().getSelfMember().getPermissions(c);
		EnumSet guildPerms = c.getGuild().getSelfMember().getPermissions();
		String jibrilPerms = "";

		if (MySQL.getTagById(c.getGuild().getOwnerId()).isPartner() && c.getGuild().getMembers().contains(Main.getJibril().getSelfUser())) {
			EnumSet JchannelPerms = Objects.requireNonNull(c.getGuild().getMember(Main.getJibril().getSelfUser())).getPermissions(c);
			EnumSet JguildPerms = Objects.requireNonNull(c.getGuild().getMember(Main.getJibril().getSelfUser())).getPermissions();
			jibrilPerms = "\n\n\n__**Permissões necessárias para uso completo da Jibril**__\n\n" +
					((JchannelPerms.contains(Permission.MANAGE_WEBHOOKS) || JguildPerms.contains(Permission.MANAGE_WEBHOOKS)) ? ":white_check_mark: -> " : ":x: -> ") + "Gerenciar webhooks\n" +
					((JchannelPerms.contains(Permission.MESSAGE_WRITE) || JguildPerms.contains(Permission.MESSAGE_WRITE)) ? ":white_check_mark: -> " : ":x: -> ") + "Escrever mensagens\n" +
					((JchannelPerms.contains(Permission.MESSAGE_MANAGE) || JguildPerms.contains(Permission.MESSAGE_MANAGE)) ? ":white_check_mark: -> " : ":x: -> ") + "Gerenciar mensagens\n" +
					((JchannelPerms.contains(Permission.MESSAGE_EMBED_LINKS) || JguildPerms.contains(Permission.MESSAGE_EMBED_LINKS)) ? ":white_check_mark: -> " : ":x: -> ") + "Inserir links\n" +
					((JchannelPerms.contains(Permission.MESSAGE_ATTACH_FILES) || JguildPerms.contains(Permission.MESSAGE_ATTACH_FILES)) ? ":white_check_mark: -> " : ":x: -> ") + "Enviar arquivos\n" +
					((JchannelPerms.contains(Permission.MESSAGE_EXT_EMOJI) || JguildPerms.contains(Permission.MESSAGE_EXT_EMOJI)) ? ":white_check_mark: -> " : ":x: -> ") + "Usar emotes externos";
		}

		return "__**Permissões necessárias para uso completo da Shiro**__\n\n" +
				(channelPerms.contains(Permission.MANAGE_CHANNEL) || guildPerms.contains(Permission.MANAGE_CHANNEL) ? ":white_check_mark: -> " : ":x: -> ") + "Gerenciar canal \n" +
				(channelPerms.contains(Permission.BAN_MEMBERS) || guildPerms.contains(Permission.BAN_MEMBERS) ? ":white_check_mark: -> " : ":x: -> ") + "Banir membros \n" +
				(channelPerms.contains(Permission.KICK_MEMBERS) || guildPerms.contains(Permission.KICK_MEMBERS) ? ":white_check_mark: -> " : ":x: -> ") + "Expulsar membros \n" +
				(channelPerms.contains(Permission.CREATE_INSTANT_INVITE) || guildPerms.contains(Permission.CREATE_INSTANT_INVITE) ? ":white_check_mark: -> " : ":x: -> ") + "Criar convite instantâneo \n" +
				(channelPerms.contains(Permission.MESSAGE_READ) || guildPerms.contains(Permission.MESSAGE_READ) ? ":white_check_mark: -> " : ":x: -> ") + "Ler mensagens \n" +
				(channelPerms.contains(Permission.MESSAGE_MANAGE) || guildPerms.contains(Permission.MESSAGE_MANAGE) ? ":white_check_mark: -> " : ":x: -> ") + "Gerenciar mensagens \n" +
				(channelPerms.contains(Permission.MESSAGE_WRITE) || guildPerms.contains(Permission.MESSAGE_WRITE) ? ":white_check_mark: -> " : ":x: -> ") + "Escrever mensagens \n" +
				(channelPerms.contains(Permission.MESSAGE_EMBED_LINKS) || guildPerms.contains(Permission.MESSAGE_EMBED_LINKS) ? ":white_check_mark: -> " : ":x: -> ") + "Inserir links \n" +
				(channelPerms.contains(Permission.MESSAGE_ATTACH_FILES) || guildPerms.contains(Permission.MESSAGE_ATTACH_FILES) ? ":white_check_mark: -> " : ":x: -> ") + "Enviar arquivos \n" +
				(channelPerms.contains(Permission.MESSAGE_HISTORY) || guildPerms.contains(Permission.MESSAGE_HISTORY) ? ":white_check_mark: -> " : ":x: -> ") + "Ver histórico de mensagens \n" +
				(channelPerms.contains(Permission.MESSAGE_ADD_REACTION) || guildPerms.contains(Permission.MESSAGE_ADD_REACTION) ? ":white_check_mark: -> " : ":x: -> ") + "Adicionar reações \n" +
				(channelPerms.contains(Permission.MESSAGE_EXT_EMOJI) || guildPerms.contains(Permission.MESSAGE_EXT_EMOJI) ? ":white_check_mark: -> " : ":x: -> ") + "Usar emotes externos \n" +
				(channelPerms.contains(Permission.MESSAGE_EXT_EMOJI) || guildPerms.contains(Permission.VOICE_CONNECT) ? ":white_check_mark: -> " : ":x: -> ") + "Conectar à canais de voz \n" +
				(channelPerms.contains(Permission.MESSAGE_EXT_EMOJI) || guildPerms.contains(Permission.VOICE_SPEAK) ? ":white_check_mark: -> " : ":x: -> ") + "Falar em canais de voz" +
				jibrilPerms;
	}

	public static <T> T getOr(T get, T or) {
		return get == null ? or : get;
	}
}
