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
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.handlers.games.Beyblade;
import com.kuuhaku.model.GamblePool;
import com.kuuhaku.model.guildConfig;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

	public static final String VOID = "\u200B";

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
		if (val < min) return min;
		else if (val > max) return max;
		else return val;
	}

	public static int clamp(int val, int min, int max) {
		if (val < min) return min;
		else if (val > max) return max;
		else return val;
	}

	public static boolean findURL(String text) {
		final Pattern urlPattern = Pattern.compile(
				".*?(?:^|[\\W])((ht|f)tp(s?)://|www\\.)(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*?)",
				Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
		text = (StringUtils.containsAny(text, ".gg", ".com", ".net", ".site", ".tech", ".br") ? "http://" : "") + text;
		text = text.replace("1", "i").replace("!", "i");
		text = text.replace("3", "e");
		text = text.replace("4", "a");
		text = text.replace("5", "s");
		text = text.replace("7", "t");
		text = text.replace("0", "o");
		text = text.replace(" ", "");

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
				log(Helper.class, LogLevel.WARN, "GIF irregular: " + imageURL);
				Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> c.sendMessage("GIF irregular: " + imageURL).queue()));
			}
		} catch (Exception e) {
			log(Helper.class, LogLevel.ERROR, "Erro ao carregar a imagem: " + e.getStackTrace()[0]);
		}
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

	public static void battle(GuildMessageReceivedEvent event) {
		if (ShiroInfo.dd.stream().noneMatch(d -> d.getP1() == event.getAuthor() || d.getP2() == event.getAuthor())) {
			return;
		}
		Beyblade.play(event);
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

	public static void log(Class source, LogLevel level, String msg) {
		final Logger logger = LogManager.getLogger(source.getName());

		switch (level) {
			case DEBUG:
				logger.debug(msg.trim());
				break;
			case INFO:
				logger.info(msg.trim());
				break;
			case WARN:
				logger.warn(msg.trim());
				break;
			case ERROR:
				logger.error(msg.trim());
				break;
			case FATAL:
				logger.fatal(msg.trim());
				break;
		}
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
			log(Helper.class, LogLevel.WARN, e + " | " + e.getStackTrace()[0]);
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

	public static boolean compareWithValues(int value, int... compareWith) {
		return Arrays.stream(compareWith).anyMatch(v -> v == value);
	}

	public static boolean containsAll(String string, String... compareWith) {
		return Arrays.stream(compareWith).allMatch(string::contains);
	}

	public static JSONObject getAPI(String directory, String value) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL("https://shiro-api.herokuapp.com/" + directory + value).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", "application/json");
		con.addRequestProperty("Accept-Charset", "UTF-8");

		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
		String input;
		StringBuilder resposta = new StringBuilder();
		while ((input = br.readLine()) != null) {
			resposta.append(input);
		}
		br.close();
		con.disconnect();

		Helper.log(Helper.class, LogLevel.DEBUG, resposta.toString());
		return new JSONObject(resposta.toString());
	}
}
