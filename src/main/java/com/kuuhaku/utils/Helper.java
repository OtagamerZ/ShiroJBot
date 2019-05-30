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
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.Beyblade;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Helper {

    public static final String VOID = "\u200B";

    private static PrivilegeLevel getPrivilegeLevel(Member member) {
        if (Main.getInfo().getNiiChan().contains(member.getUser().getId())) {
            return PrivilegeLevel.NIICHAN;
        } else if (Main.getInfo().getDevelopers().contains(member.getUser().getId())) {
            return PrivilegeLevel.OWNER;
        } else if (member.hasPermission(Permission.MESSAGE_MANAGE)) {
            return PrivilegeLevel.STAFF;
        }
        return PrivilegeLevel.USER;
    }

    public static boolean hasPermission(Member member, PrivilegeLevel privilegeLevel) {
        return getPrivilegeLevel(member) != PrivilegeLevel.USER || privilegeLevel == PrivilegeLevel.USER;
    }
	
	/*public static String formatMessage(String message, String commandName, User user) {
		return message.replaceAll("%CMD_NAME%", RegexUtils.escapeString(commandName)).replaceAll("%USER_NAME%", "<@" + user.getId() + ">");
	}*/

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static File createOhNoImage(String text) throws IOException {
        BufferedImage image = ImageIO.read(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("ohno.png")));

        BufferedImage resultImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D w = (Graphics2D) resultImg.getGraphics();
        w.drawImage(image, 0, 0, null);
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);
        w.setComposite(alphaChannel);
        w.setColor(Color.BLACK);
        w.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));

        FontMetrics fontMetrics = w.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(text, w);
        
        /*int centerX = (image.getWidth() - (int) rect.getWidth()) / 2;
        int centerY = image.getHeight() / 2;*/

        //21 - 123456789012345678901
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String str : text.split(" ")) {
            if (count + str.length() > 21) {
                sb.append("\n");
                count = 0;
                if (str.length() > 21) {
                    String newStr = str;
                    do {
                        sb.append(newStr, 0, 20).append("\n");
                        newStr = newStr.substring(21);
                    } while (newStr.length() > 21);
                    count = 0;
                    continue;
                }
            }
            sb.append(str).append(" ");
            count += str.length() + 1;
        }
        text = sb.toString().trim();
        int lineN = 1;
        for (String line : text.split("\n")) {
            if (lineN > 4) break;
            w.drawString(line, 344 + 3, (int) (22 + (rect.getHeight() * lineN)));
            lineN++;
        }

        File result = new File(System.getProperty("user.dir") + "\\ohno-" + Instant.now().toEpochMilli() + ".png");
        ImageIO.write(resultImg, "png", result);
        w.dispose();

        return result;
    }

    public static String downloadWebPage(String webpage) throws Exception {
        URL url = new URL(webpage);
        BufferedReader rdr = new BufferedReader(new InputStreamReader(url.openStream()));

        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = rdr.readLine()) != null)
            sb.append(line).append("\n");

        rdr.close();

        return sb.toString();
    }

    public static void sendPM(User user, String message) {
        user.openPrivateChannel().queue((channel) -> channel.sendMessage(message).queue());
    }

    public static void purge(MessageChannel channel, int num) {
        MessageHistory history = new MessageHistory(channel);
        history.retrievePast(num).queue(channel::purgeMessages);
    }

    public static String getCustomEmoteMention(Guild guild, String name) {
        for (Emote em : guild.getEmotes()) {
            if (em.getName().equalsIgnoreCase(name))
                return em.getAsMention();
        }
        return null;
    }

    public static Emote getCustomEmote(Guild guild, String name) {
        for (Emote em : guild.getEmotes()) {
            if (em.getName().equalsIgnoreCase(name))
                return em;
        }
        return null;
    }

    public static void typeMessage(MessageChannel channel, String message) {
        channel.sendTyping().queue(tm -> channel.sendMessage(message).queueAfter(message.length() * 25 > 10000 ? 10000 : message.length(), TimeUnit.MILLISECONDS));
    }

    public static void sendReaction(MessageChannel channel, String message, InputStream is, boolean reacted) {
        if (reacted)
            channel.sendMessage(message).addFile(is, "reaction.gif").queue(m -> m.addReaction("\u21aa").queue());
        else channel.sendMessage(message).addFile(is, "reaction.gif").queue();
    }

    public static void cls() {
        try {
            final String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                Runtime.getRuntime().exec("cls");
            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (final Exception e) {
            System.out.println("Erro ao limpar o console.");
        }
    }

    public static float getDefFac(boolean defending, Beyblade b) {
        if (defending) {
            if (b.getS() == null) {
                return b.getStability();
            } else {
                if (b.getS().isBear()) {
                    return 2.0f + (b.getStability() / 2);
                } else {
                    return b.getStability();
                }
            }
        } else {
            return 1.0f;
        }
    }

    public static int rng(int maxValue) {
        return Math.abs(new Random().nextInt(maxValue));
    }

    public static Color colorThief(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestProperty("User-Client", "Mozilla/5.0");
        BufferedImage icon = ImageIO.read(con.getInputStream());

        return new Color(ColorThief.getColor(icon)[0], ColorThief.getColor(icon)[1], ColorThief.getColor(icon)[2]);
    }

    public static void embedConfig(Message message) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Helper.colorThief(message.getGuild().getIconUrl()));
        eb.setTitle("⚙ | Configurações do servidor");
        eb.addField("\uD83D\uDDDD » Prefixo", SQLite.getGuildPrefix(message.getGuild().getId()) + "`", true);
        eb.addBlankField(true);
        eb.addBlankField(true);
        eb.addField("\uD83D\uDCD6 » Canal Boas-vindas", SQLite.getGuildCanalBV(message.getGuild().getId(), true), true);
        eb.addField("\uD83D\uDCDD » Mensagem de Boas-vindas", SQLite.getGuildMsgBV(message.getGuild().getId(), true), true);
        eb.addField("\uD83D\uDCD6 » Canal Adeus", SQLite.getGuildCanalAdeus(message.getGuild().getId(), true), true);
        eb.addField("\uD83D\uDCDD » Mensagem de Adeus", SQLite.getGuildMsgAdeus(message.getGuild().getId(), true), true);
//				eb.addField("Enviado em:", df.format(message.getCreationTime()), true);
//				eb.addField("Relatório:", "```" + mensagem + "```", false);

        message.getTextChannel().sendMessage(eb.build()).queue();
    }
}
