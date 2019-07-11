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
 *     along with Shiro J Bot.  If not, see <http://www.gnu.org/licenses/>
 */

package com.kuuhaku.model;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Profile {
	public static Font FONT;

	static {
		try {
			FONT = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Profile.class.getClassLoader().getResourceAsStream("font/Doreking.ttf")));
		} catch (FontFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	public static ByteArrayOutputStream makeProfile(net.dv8tion.jda.core.entities.Member m, Guild g) throws IOException {
		int w = 944;
		int h = 600;

		HttpURLConnection con = (HttpURLConnection) new URL(m.getUser().getAvatarUrl()).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		BufferedImage avatar = scaleImage(ImageIO.read(con.getInputStream()), 200, 200);

		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		con = (HttpURLConnection) new URL(SQLite.getMemberById(m.getUser().getId() + g.getId()).getBg()).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		g2d.drawImage(scaleImage(ImageIO.read(con.getInputStream()), bi.getWidth(), bi.getHeight()), null, 0, 0);

		Color main = Helper.reverseColor(Helper.colorThief(SQLite.getMemberById(m.getUser().getId() + g.getId()).getBg()));


		g2d.setColor(new Color(main.getRed(), main.getGreen(), main.getBlue(), 100));
		g2d.fillRect(0, 300, w, 300);
		g2d.fillRect(0, 350, w, 250);

		g2d.setColor(new Color(0, 255, 0));
		g2d.setClip(new Rectangle2D.Float(0, 100, w, 250));
		g2d.fillArc(40, 190, avatar.getWidth() + 20, avatar.getHeight() + 20, 210, (SQLite.getMemberById(m.getUser().getId() + g.getId()).getXp() * 240) / ((int) Math.pow(SQLite.getMemberById(m.getUser().getId() + g.getId()).getLevel(), 2) * 100) * -1);

		g2d.setColor(main);
		g2d.setClip(null);
		g2d.fillRect(52, 350, 196, 200);

		g2d.setColor(new Color(100, 100, 100, 150));
		g2d.fillRect(268, 370, 177, 177);
		g2d.fillRect(466, 370, 455, 177);

		g2d.setColor(new Color(100, 100, 100));
		g2d.setStroke(new BasicStroke(5));
		g2d.drawRect(268, 370, 177, 177);
		g2d.drawRect(466, 370, 455, 177);
		for (int i = 1; i < 3; i++) {
			g2d.drawLine(268 + (177 / 3 * i), 370, 268 + (177 / 3 * i), 547);
			g2d.drawLine(268, 370 + (177 / 3 * i), 445, 370 + (177 / 3 * i));
		}

		int luma = ((int) Math.round(0.2126 * main.getRed() + 0.7152 * main.getGreen() + 0.0722 * main.getBlue()));
		if (luma >= 128) g2d.setColor(Color.BLACK);
		else g2d.setColor(Color.WHITE);
		g2d.setFont(new Font(FONT.getName(), Font.PLAIN, 50));
		printCenteredString("LEVEL", 196, 52, 440, g2d);
		String name = m.getEffectiveName();
		if ((int) g2d.getFontMetrics().getStringBounds(m.getEffectiveName(), g2d).getWidth() >= 678)
			name = m.getEffectiveName().substring(0, 21).concat("...");
		g2d.drawString(name, 270, 342);

		g2d.setFont(new Font(FONT.getName(), Font.BOLD, 85));
		printCenteredString(String.valueOf(SQLite.getMemberById(m.getUser().getId() + g.getId()).getLevel()), 196, 52, 515, g2d);

		g2d.setFont(new Font(FONT.getName(), Font.PLAIN, 25));
		printCenteredString(SQLite.getMemberById(m.getUser().getId() + g.getId()).getXp() + "/" + ((int) Math.pow(SQLite.getMemberById(m.getUser().getId() + g.getId()).getLevel(), 2) * 100), 196, 52, 538, g2d);

		List<Member> mbs = SQLite.getMemberRank(g.getId(), false);
		int pos = 0;
		for (int i = 0; i < mbs.size(); i++) {
			if (mbs.get(i).getId().equals(m.getUser().getId() + g.getId())) {
				pos = i + 1;
				break;
			}
		}

		mbs = SQLite.getMemberRank(g.getId(), true);
		int posG = 0;
		for (int i = 0; i < mbs.size(); i++) {
			if (mbs.get(i).getId().equals(m.getUser().getId() + g.getId())) {
				posG = i + 1;
				break;
			}
		}

		g2d.setFont(new Font(FONT.getName(), Font.PLAIN, 40));
		printCenteredString("Emblemas", 182, 266, 590, g2d);
		printCenteredString("Biografia", 460, 466, 590, g2d);

		g2d.setFont(new Font(FONT.getName(), Font.PLAIN, 30));
		printCenteredString("Rank: #" + pos + "/#" + posG, 196, 52, 585, g2d);


		g2d.setFont(new Font("DejaVu Sans", Font.BOLD, 25));
		String s = SQLite.getMemberById(m.getUser().getId() + g.getId()).getBio();
		drawStringMultiLine(g2d, s, 440, 474, 403);

		drawBadges(m, g, g2d);

		g2d.setClip(new Ellipse2D.Float(50, 200, avatar.getWidth(), avatar.getHeight()));
		g2d.fillOval(50, 200, avatar.getWidth(), avatar.getHeight());
		g2d.drawImage(avatar, null, 50, 200);

		g2d.dispose();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(clipRoundEdges(bi), "png", baos);

		return baos;
	}

	private static BufferedImage clipRoundEdges(BufferedImage image) {
		BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();

		g2d.setClip(new RoundRectangle2D.Float(0, 0, bi.getWidth(), bi.getHeight(), 80, 80));
		g2d.drawImage(image, null, 0, 0);
		g2d.dispose();

		return bi;
	}

	private static void drawBadges(net.dv8tion.jda.core.entities.Member m, Guild s, Graphics2D g2d) throws IOException {
		java.util.List<BufferedImage> badges = new ArrayList<BufferedImage>() {{
			if (m.getUser().getId().equals(Main.getInfo().getNiiChan()) || Main.getInfo().getDevelopers().contains(m.getUser().getId()))
				add(ImageIO.read(Objects.requireNonNull(Profile.class.getClassLoader().getResource("icons/dev.png"))));
			if (Main.getInfo().getEditors().contains(m.getUser().getId()))
				add(ImageIO.read(Objects.requireNonNull(Profile.class.getClassLoader().getResource("icons/writer.png"))));
			try {
				if (MySQL.getTagById(m.getUser().getId()).isPartner())
					add(ImageIO.read(Objects.requireNonNull(Profile.class.getClassLoader().getResource("icons/partner.png"))));
			} catch (NoResultException ignore) {
			}
			if (m.hasPermission(Permission.MANAGE_CHANNEL))
				add(ImageIO.read(Objects.requireNonNull(Profile.class.getClassLoader().getResource("icons/mod.png"))));
			try {
				if (MySQL.getChampionBeyblade().getId().equals(m.getUser().getId()))
					add(ImageIO.read(Objects.requireNonNull(Profile.class.getClassLoader().getResource("icons/champ.png"))));
			} catch (NoResultException ignore) {
			}
			try {
				if (SQLite.getMemberById(m.getUser().getId() + s.getId()).getLevel() >= 20)
					add(ImageIO.read(Objects.requireNonNull(Profile.class.getClassLoader().getResource("icons/veteran.png"))));
			} catch (NoResultException ignore) {
			}
			try {
				if (MySQL.getTagById(m.getUser().getId()).isVerified())
					add(ImageIO.read(Objects.requireNonNull(Profile.class.getClassLoader().getResource("icons/verified.png"))));
			} catch (NoResultException ignore) {
			}
			try {
				if (MySQL.getTagById(m.getUser().getId()).isToxic())
					add(ImageIO.read(Objects.requireNonNull(Profile.class.getClassLoader().getResource("icons/toxic.png"))));
			} catch (NoResultException ignore) {
			}
			try {
				if (!SQLite.getMemberById(m.getUser().getId() + s.getId()).getWaifu().isEmpty()) {
					add(ImageIO.read(Objects.requireNonNull(Profile.class.getClassLoader().getResource("icons/married.png"))));
					g2d.setFont(new Font(FONT.getName(), Font.PLAIN, 30));
					g2d.drawString("Casado(a) com: " + Main.getInfo().getUserByID(SQLite.getMemberById(m.getUser().getId() + s.getId()).getWaifu()).getName(), 270, 298);
				}
			} catch (NoResultException ignore) {
			}
		}};

		List<int[]> coords = new ArrayList<int[]>() {{
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					add(new int[]{276 + (59 * x), 378 + (59 * y)});
				}
			}
		}};

		for (int i = 0; i < badges.size(); i++) {
			g2d.drawImage(scaleImage(badges.get(i), 44, 44), null, coords.get(i)[0], coords.get(i)[1]);
		}
	}

	public static BufferedImage scaleImage(BufferedImage image, int w, int h) {

		// Make sure the aspect ratio is maintained, so the image is not distorted
		double thumbRatio = (double) w / (double) h;
		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);
		double aspectRatio = (double) imageWidth / (double) imageHeight;

		if (thumbRatio > aspectRatio) {
			h = (int) (w / aspectRatio);
		} else {
			w = (int) (h * aspectRatio);
		}

		// Draw the scaled image
		BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = newImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, w, h, null);

		return newImage;
	}

	public static void printCenteredString(String s, int width, int XPos, int YPos, Graphics2D g2d) {
		int stringLen = (int) g2d.getFontMetrics().getStringBounds(s, g2d).getWidth();
		int start = width / 2 - stringLen / 2;
		g2d.drawString(s, start + XPos, YPos);
	}

	public static void drawStringMultiLine(Graphics2D g, String text, int lineWidth, int x, int y) {
		FontMetrics m = g.getFontMetrics();
		if (m.stringWidth(text) < lineWidth) {
			g.drawString(text, x, y);
		} else {
			String[] words = text.split(" ");
			StringBuilder currentLine = new StringBuilder(words[0]);
			for (int i = 1; i < words.length; i++) {
				if (m.stringWidth(currentLine + words[i]) < lineWidth) {
					currentLine.append(" ").append(words[i]);
				} else {
					String s = currentLine.toString();
					g.drawString(s, x, y);
					y += m.getHeight();
					currentLine = new StringBuilder(words[i]);
				}
			}
			if (currentLine.toString().trim().length() > 0) {
				g.drawString(currentLine.toString(), x, y);
			}
		}
	}
}
