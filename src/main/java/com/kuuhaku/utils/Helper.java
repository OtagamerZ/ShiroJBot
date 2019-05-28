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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import com.kuuhaku.Main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Helper {
	
	public static final String VOID = "\u200B";
	
	public static PrivilegeLevel getPrivilegeLevel(Member member) {
		if (Main.getInfo().getDevelopers().contains(member.getUser().getId())) {
			return PrivilegeLevel.OWNER;
		} else if(member.hasPermission(Permission.MESSAGE_MANAGE)) {
			return PrivilegeLevel.STAFF;
		}
		return PrivilegeLevel.USER;
	}
	
	public static boolean hasPermission(Member member, PrivilegeLevel privilegeLevel) {
		if(getPrivilegeLevel(member) == PrivilegeLevel.USER && privilegeLevel != PrivilegeLevel.USER) { return false; }
		return getPrivilegeLevel(member) != PrivilegeLevel.STAFF || privilegeLevel != PrivilegeLevel.OWNER;
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
        for(String str : text.split(" ")) {
        	if(count + str.length() > 21) {
        		sb.append("\n");
        		count = 0;
        		if(str.length()>21) {
        			String newStr = str;
        			do {
        				sb.append(newStr, 0, 20).append("\n");
        				newStr = newStr.substring(21);
        			}while(newStr.length()>21);
        			count=0;
        			continue;
        		}
        	}
        	sb.append(str).append(" ");
        	count+=str.length()+1;
        }
        text = sb.toString().trim();
        int lineN = 1;
        for(String line : text.split("\n")) {
        	if(lineN>4)
        		break;
        	w.drawString(line, 344+3, (int) (22+(rect.getHeight()*lineN)));
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
		user.openPrivateChannel().queue( (channel) -> channel.sendMessage(message).queue() );
	}

	public static void sendChannelMessageDeleteAfter(MessageChannel channel, String message, long delay, TimeUnit unit) {
		channel.sendMessage(message).queue((msg) -> msg.delete().queueAfter(delay, unit));
	}

	public static void purge(MessageChannel channel, int num) {
		MessageHistory history = new MessageHistory(channel);
		history.retrievePast(num).queue(channel::purgeMessages);
	}

	public static String getCustomEmoteMention(Guild guild, String name) {
		for(Emote em : guild.getEmotes()) {
			if(em.getName().equalsIgnoreCase(name))
				return em.getAsMention();
		}
		return null;
	}

	public static Emote getCustomEmote(Guild guild, String name) {
		for(Emote em : guild.getEmotes()) {
			if(em.getName().equalsIgnoreCase(name))
				return em;
		}
		return null;
	}

	public static void typeMessage(MessageChannel channel, String message) {
		channel.sendTyping().queue(tm -> channel.sendMessage(message).queueAfter(message.length() * 25 > 10000 ? 10000 : message.length(), TimeUnit.MILLISECONDS));
	}

	public static void sendReaction(MessageChannel channel, String message, InputStream is, boolean reacted) {
		if (reacted) channel.sendMessage(message).addFile(is, "reaction.gif").queue(m -> m.addReaction("\u21aa").queue());
		else channel.sendMessage(message).addFile(is, "reaction.gif").queue();
	}
}
