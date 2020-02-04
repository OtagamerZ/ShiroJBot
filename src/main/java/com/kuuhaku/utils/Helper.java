/*
 * This file is part of Shiro J Bot.
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
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.TagDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.common.Extensions;
import com.kuuhaku.model.common.GamblePool;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Tags;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Helper {

	public static final String VOID = "\u200B";
	public static final String CANCEL = "\u274E";
	public static final String ACCEPT = "\u2705";
	public static final int CANVAS_SIZE = 1024;
	public static final DateTimeFormatter dateformat = DateTimeFormatter.ofPattern("dd/MMM/yyyy | HH:mm:ss (z)");
	public static final String HOME = "674261700366827539";


	private static PrivilegeLevel getPrivilegeLevel(Member member) {
		if (Main.getInfo().getNiiChan().contains(member.getId())) {
			return PrivilegeLevel.NIICHAN;
		} else if (Main.getInfo().getDevelopers().contains(member.getId())) {
			return PrivilegeLevel.DEV;
		} else if (Main.getInfo().getSheriffs().contains(member.getId())) {
			return PrivilegeLevel.SHERIFF;
		} else if (member.hasPermission(Permission.MESSAGE_MANAGE)) {
			return PrivilegeLevel.MOD;
		} else if (TagDAO.getTagById(member.getId()).isPartner()) {
			return PrivilegeLevel.PARTNER;
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

	public static List<String> getGamble() {
		GamblePool gp = new GamblePool();
		String[] icon = {":cheese:", ":izakaya_lantern:", ":moneybag:", ":diamond_shape_with_a_dot_inside:", ":rosette:", "<a:Wow:598497560734203926>"};
		for (int i = 0; i < icon.length; i++) {
			gp.addGamble(new GamblePool.Gamble(icon[i], icon.length - i));
		}
		String[] pool = gp.getPool();
		List<String> result = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			result.add(pool[clamp(rng(pool.length), 0, pool.length - 1)]);
		}
		return result;
	}

	public static void spawnAd(MessageChannel channel) {
		if (Helper.rng(1000) > 990) {
			channel.sendMessage("Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\nhttps://top.gg/bot/572413282653306901").queue();
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
			sendPM(Objects.requireNonNull(chn.getGuild().getOwner()).getUser(), ":x: | " + name + " não possui permissão para criar um webhook em seu servidor");
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
		return new Color(rng(255), rng(255), rng(255));
	}

	public static boolean compareWithValues(int value, int... compareWith) {
		return Arrays.stream(compareWith).anyMatch(v -> v == value);
	}

	public static boolean containsAll(String string, String... compareWith) {
		return Arrays.stream(compareWith).allMatch(string::contains);
	}

	public static boolean containsAny(String string, String... compareWith) {
		return Arrays.stream(compareWith).anyMatch(string::contains);
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
				EnumSet<Permission> perms = Objects.requireNonNull(c.getGuild().getMemberById(jibril.getId())).getPermissions(c);

				jibrilPerms = "\n\n\n__**Permissões atuais da Jibril**__\n\n" +
						perms.stream().map(p -> ":white_check_mark: -> " + p.getName() + "\n").sorted().collect(Collectors.joining());
			}
		} catch (NoResultException ignore) {
		}

		Member shiro = c.getGuild().getSelfMember();
		EnumSet<Permission> perms = shiro.getPermissions(c);

		return "__**Permissões atuais da Shiro**__\n\n" +
				perms.stream().map(p -> ":white_check_mark: -> " + p.getName() + "\n").sorted().collect(Collectors.joining()) +
				jibrilPerms;
	}

	public static <T> T getOr(T get, T or) {
		return get == null || (get instanceof String && ((String) get).isEmpty()) ? or : get;
	}

	public static boolean hasRoleHigherThan(Member user, Member target) {
		List<Role> usrRoles = user.getRoles().stream().sorted(Comparator.comparingInt(Role::getPosition)).collect(Collectors.toList());
		List<Role> tgtRoles = target.getRoles().stream().sorted(Comparator.comparingInt(Role::getPosition)).collect(Collectors.toList());

		return usrRoles.get(0).getPosition() < tgtRoles.get(0).getPosition();
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

	public static String getResponse(HttpURLConnection con) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));

		String input;
		StringBuilder resposta = new StringBuilder();
		while ((input = br.readLine()) != null) {
			resposta.append(input);
		}
		br.close();
		con.disconnect();

		return resposta.toString();
	}

	public static void nonPartnerAlert(User author, Member member, MessageChannel channel, String s, String link) {
		try {
			if (!TagDAO.getTagById(author.getId()).isPartner() && !hasPermission(member, PrivilegeLevel.DEV)) {
				channel.sendMessage(":x: | Este comando é exlusivo para parceiros!").queue();
				return;
			}
		} catch (NoResultException e) {
			channel.sendMessage(":x: | Este comando é exlusivo para parceiros!").queue();
			return;
		}

		channel.sendMessage("Link enviado no privado!").queue();

		EmbedBuilder eb = new EmbedBuilder();

		eb.setThumbnail("https://www.pacific.edu/Images/library/Renovation%20Renderings/LogoMakr_2mPTly.png");
		eb.setTitle("Olá, obrigado por apoiar meu desenvolvimento!");
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
			Map<String, BiConsumer<Member, Message>> buttons = new HashMap<>();

			TextChannel channel = g.getTextChannelById(jo.getString("canalId"));

			if (channel == null) {
				ja.remove(jo.getString("canalId"));
				gc.setButtonConfigs(ja);
				GuildDAO.updateGuildSettings(gc);
			} else try {
				if (k.equals("gatekeeper")) {
					Message msg = channel.retrieveMessageById(jo.getString("msgId")).submit().get();
					resolveButton(g, jo, buttons);

					buttons.put("\uD83D\uDEAA", (m, v) -> {
						try {
							m.kick("Não aceitou as regras.").queue();
						} catch (InsufficientPermissionException ignore) {
						}
					});

					Pages.buttonize(Main.getInfo().getAPI(), msg, buttons, false);

				} else {
					Message msg = channel.retrieveMessageById(jo.getString("msgId")).submit().get();
					resolveButton(g, jo, buttons);

					buttons.put(CANCEL, (m, ms) -> {
						if (m.getUser().getId().equals(jo.getString("author"))) {
							JSONObject gcjo = gc.getButtonConfigs();
							gcjo.remove(jo.getString("msgId"));
							gc.setButtonConfigs(gcjo);
							GuildDAO.updateGuildSettings(gc);
							ms.clearReactions().queue();
						}
					});

					Pages.buttonize(Main.getInfo().getAPI(), msg, buttons, true);
				}
			} catch (NullPointerException | ErrorResponseException | InterruptedException | ExecutionException e) {
				Helper.logger(Helper.class).error(e + " | " + e.getStackTrace()[0]);
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

				Pages.buttonize(Main.getInfo().getAPI(), msg, buttons, false);
			});
		});
	}

	public static void addButton(String[] args, Message message, MessageChannel channel, GuildConfig gc, String s2, boolean gatekeeper) {
		JSONObject root = gc.getButtonConfigs();
		String msgId = channel.retrieveMessageById(args[0]).complete().getId();

		JSONObject msg = new JSONObject();

		JSONObject btn = new JSONObject();
		btn.put("emote", EmojiUtils.containsEmoji(s2) ? s2 : Objects.requireNonNull(Main.getInfo().getAPI().getEmoteById(s2)).getId());
		btn.put("role", message.getMentionedRoles().get(0).getId());

		channel.retrieveMessageById(msgId).queue();

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
	}

	public static String getSponsors() {
		List<String> sponsors = TagDAO.getSponsors().stream().map(Tags::getId).collect(Collectors.toList());
		List<Guild> spGuilds = Main.getInfo().getAPI().getGuilds().stream().filter(g -> sponsors.contains(g.getOwnerId()) && g.getSelfMember().hasPermission(Permission.CREATE_INSTANT_INVITE)).collect(Collectors.toList());
		StringBuilder sb = new StringBuilder();

		for (Guild g : spGuilds) {
			AtomicReference<Invite> i = new AtomicReference<>();
			g.retrieveInvites().queue(invs -> invs.forEach(inv -> {
				if (inv.getInviter() == Main.getInfo().getAPI().getSelfUser()) {
					i.set(inv);
				}
			}));

			if (i.get() == null) {
				try {
					sb.append(Helper.createInvite(g).setMaxAge(0).submit().get().getUrl()).append("\n");
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
}
