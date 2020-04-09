/*
 * This file is part of Shiro J Bot.
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be	 useful,
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
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.command.commands.reactions.Reaction;
import com.kuuhaku.controller.postgresql.LogDAO;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.common.Extensions;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Log;
import com.kuuhaku.model.persistent.Tags;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.python.google.common.collect.Lists;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
        if (Main.getInfo().getNiiChan().equals(member.getId()))
            return PrivilegeLevel.NIICHAN;
        else if (Main.getInfo().getDevelopers().contains(member.getId()))
            return PrivilegeLevel.DEV;
        else if (Main.getInfo().getSheriffs().contains(member.getId()))
            return PrivilegeLevel.SHERIFF;
        else if (member.hasPermission(Permission.MESSAGE_MANAGE))
            return PrivilegeLevel.MOD;
        else if (TagDAO.getTagById(member.getGuild().getOwnerId()).isPartner())
            return PrivilegeLevel.PARTNER;
        else if (member.getRoles().stream().anyMatch(r -> StringUtils.containsIgnoreCase(r.getName(), "dj")))
            return PrivilegeLevel.DJ;
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
        text = StringUtils.deleteWhitespace(text);
        text = (Extensions.checkExtension(text) ? "http://" : "") + text;
        text = text.replace("1", "i").replace("!", "i");
        text = text.replace("3", "e");
        text = text.replace("4", "a");
        text = text.replace("5", "s");
        text = text.replace("7", "t");
        text = text.replace("0", "o");
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

    public static Consumer<MessageAction> sendReaction(Reaction r, String imageURL, MessageChannel channel, boolean allowReact) throws IllegalAccessException {
        try {
            if (r.isAnswerable() && allowReact) {
                return act -> act.queue(m -> Pages.buttonize(Main.getInfo().getAPI(), m, Collections.singletonMap("↪", (mb, msg) -> r.answer((TextChannel) channel)), false, 60, TimeUnit.SECONDS));
            } else
                return RestAction::queue;
        } catch (Exception e) {
            Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> c.sendMessage("GIF com erro: " + imageURL).queue()));
            logger(Helper.class).error("Erro ao carregar a imagem: " + imageURL + " -> " + e + " | " + e.getStackTrace()[0]);
            throw new IllegalAccessException();
        }
    }

    public static int rng(int maxValue) {
        return Math.abs(new Random().nextInt(maxValue));
    }

    public static Color colorThief(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedImage icon = ImageIO.read(con.getInputStream());

        if (icon != null)
            return new Color(ColorThief.getColor(icon)[0], ColorThief.getColor(icon)[1], ColorThief.getColor(icon)[2]);
        else return getRandomColor();
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
        return new Color(rng(255), rng(255), rng(255));
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
                if (emt.size() > 0) args[i] = emt.get(Helper.rng(emt.size())).getAsMention();
            }
        }

        return String.join(" ", args);
    }

    public static void drawString(Graphics2D g, String text, int x, int y, int width) {
        StringBuilder sb = new StringBuilder();
        List<String> lines = new ArrayList<>();
        for (String word : text.split(" ")) {
            if (g.getFontMetrics().stringWidth(sb.toString() + word) > width) {
                lines.add(sb.toString().trim());
                sb.setLength(0);
            }
            sb.append(word).append(" ");
        }
        if (sb.length() > 0) lines.add(sb.toString());
        if (lines.size() == 0) lines.add(text);

        for (int i = 0; i < lines.size(); i++) {
            g.drawString(lines.get(i), x, y + (g.getFontMetrics().getHeight() * i));
        }
    }

    public static ByteArrayOutputStream renderMeme(String text, BufferedImage bi) throws IOException {
        String[] lines = text.split("\\r?\\n");
        int canvasSize = 2;

        BufferedImage canvas = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = canvas.createGraphics();

        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        for (String line : lines) {
            canvasSize += g2d.getFontMetrics().stringWidth(line) > bi.getWidth() ? 1 : 0;
        }

        canvas = new BufferedImage(bi.getWidth(), (30 * ((lines.length - 1) + canvasSize) + (6 * lines.length)) + 15 + bi.getHeight(), BufferedImage.TYPE_INT_RGB);
        g2d = canvas.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        for (int i = 0; i < lines.length; i++) drawString(g2d, lines[i], 25, 45 + (45 * i), bi.getWidth() - 50);
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
            } catch (IndexOutOfBoundsException ex) {
                try {
                    e = Main.getInfo().getAPI().getEmotesByName(oldWords[i].replace("&", ""), true).get(0);
                    makenew = true;
                } catch (IndexOutOfBoundsException exc) {
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
            if (s.isEmpty()) empty = true;
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
        if (author == Main.getInfo().getSelfUser() && command.getCategory().isBotBlocked()) {
            channel.sendMessage(":x: | Não posso executar este comando, apenas usuários humanos podem usar ele.").queue();
            return true;
        } else if (!hasPermission(guild.getSelfMember(), Permission.MESSAGE_MANAGE, (TextChannel) channel) && GuildDAO.getGuildById(guild.getId()).isServerMMLocked() && command.requiresMM()) {
            channel.sendMessage(":x: | Para que meus comandos funcionem corretamente, preciso da permissão de gerenciar mensagens.\nPor favor contate um moderador ou administrador desse servidor para que me dê essa permissão.").queue();
            return true;
        }

        LogDAO.saveLog(new Log().setGuildId(guild.getId()).setGuild(guild.getName()).setUser(author.getAsTag()).setCommand(rawMessage));
        logToChannel(author, true, command, "Um comando foi usado no canal " + ((TextChannel) channel).getAsMention(), guild, rawMessage);
        return false;
    }

    public static float prcnt(float value, float max) {
        return (value * 100) / max;
    }

    public static boolean checkPermissions(User author, Member member, Message message, MessageChannel channel, Guild guild, String prefix, String rawMsgNoPrefix, String[] args, Command command) {
        if (command.getCategory() == Category.NSFW && !((TextChannel) channel).isNSFW()) {
            try {
                channel.sendMessage(":x: | Este comando está categorizado como NSFW, por favor use-o em um canal apropriado!").queue();
                return true;
            } catch (InsufficientPermissionException ignore) {
            }
            return false;
        } else if (!hasPermission(member, command.getCategory().getPrivilegeLevel())) {
            try {
                channel.sendMessage(":x: | Você não tem permissão para executar este comando!").queue();
                return true;
            } catch (InsufficientPermissionException ignore) {
            }
            return false;
        }

        command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, prefix);
        spawnAd(channel);
        return true;
    }
}
