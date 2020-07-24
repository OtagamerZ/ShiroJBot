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

package com.kuuhaku.model.common;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.Tag;
import com.kuuhaku.utils.TagIcons;
import net.dv8tion.jda.api.entities.Guild;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Profile {
	public static Font FONT;
	public static final int WIDTH = 944;
	public static final int HEIGTH = 600;

	static {
		try {
			FONT = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Profile.class.getClassLoader().getResourceAsStream("font/Doreking.ttf")));
		} catch (FontFormatException | IOException e) {
			Helper.logger(Profile.class).error(e + " | " + e.getStackTrace()[0]);
		}
	}

	public static ByteArrayOutputStream makeProfile(net.dv8tion.jda.api.entities.Member m, Guild g) throws IOException {
		int w = WIDTH;
		BufferedImage avatar;
		Member mb = MemberDAO.getMemberById(m.getUser().getId() + g.getId());

		try {
			avatar = Helper.scaleImage(ImageIO.read(Helper.getImage(m.getUser().getEffectiveAvatarUrl())), 200, 200);
		} catch (NullPointerException | IOException e) {
			avatar = Helper.scaleImage(ImageIO.read(Helper.getImage("https://institutogoldenprana.com.br/wp-content/uploads/2015/08/no-avatar-25359d55aa3c93ab3466622fd2ce712d1.jpg")), 200, 200);
		}

		BufferedImage bi = new BufferedImage(w, HEIGTH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setBackground(Color.black);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int xOffset = 0;
		int yOffset = 0;

		Color main;
		try {
			BufferedImage bg = Helper.scaleImage(ImageIO.read(Helper.getImage(mb.getBg())), bi.getWidth(), bi.getHeight());

			if (bg.getWidth() > bi.getWidth()) xOffset = -(bg.getWidth() - bi.getWidth()) / 2;
			if (bg.getHeight() > bi.getHeight()) yOffset = -(bg.getHeight() - bi.getHeight()) / 2;

			g2d.drawImage(bg, xOffset, yOffset, null);
			main = Helper.reverseColor(Helper.colorThief(mb.getBg()));
		} catch (IOException e) {
			BufferedImage bg = Helper.scaleImage(ImageIO.read(Helper.getImage("https://pm1.narvii.com/6429/7f50ee6d5a42723882c6c23a8420f24dfff60e4f_hq.jpg")), bi.getWidth(), bi.getHeight());

			if (bg.getWidth() > bi.getWidth()) xOffset = -(bg.getWidth() - bi.getWidth()) / 2;
			if (bg.getHeight() > bi.getHeight()) yOffset = -(bg.getHeight() - bi.getHeight()) / 2;

			g2d.drawImage(bg, xOffset, yOffset, null);
			main = Helper.reverseColor(Helper.colorThief("https://pm1.narvii.com/6429/7f50ee6d5a42723882c6c23a8420f24dfff60e4f_hq.jpg"));
		}

		Color lvlBar;
		if (Helper.between(mb.getLevel(), 0, 35)) {
			lvlBar = Color.decode("#552911");
		} else if (Helper.between(mb.getLevel(), 35, 65)) {
			lvlBar = Color.decode("#b3b3b3");
		} else if (Helper.between(mb.getLevel(), 65, 90)) {
			lvlBar = Color.decode("#cf9401");
		} else {
			lvlBar = Color.decode("#00d3d3");
		}

		g2d.setColor(new Color(main.getRed(), main.getGreen(), main.getBlue(), 100));
		g2d.fillRect(0, 300, w, 300);
		g2d.fillRect(0, 350, w, 250);

		g2d.setColor(Color.black);
		g2d.fillOval(38, 188, avatar.getWidth() + 24, avatar.getHeight() + 24);
		g2d.fillRect(50, 348, 200, 204);

		g2d.setColor(lvlBar);
		g2d.setClip(new Rectangle2D.Float(0, 100, w, 250));
		g2d.fillArc(40, 190, avatar.getWidth() + 20, avatar.getHeight() + 20, 210, (int) ((mb.getXp() * 240) / ((int) Math.pow(mb.getLevel(), 2) * 100) * -1));

		g2d.setColor(main);
		g2d.setClip(null);
		g2d.fillRect(52, 350, 196, 200);

		g2d.setColor(new Color(0, 0, 0, 150));
		g2d.fillRect(268, 370, 177, 177);
		g2d.fillRect(466, 370, 455, 177);

		g2d.setColor(new Color(0, 0, 0));
		g2d.setStroke(new BasicStroke(3));
		g2d.drawRect(268, 370, 177, 177);
		g2d.drawRect(466, 370, 455, 177);
		for (int i = 1; i < 3; i++) {
			g2d.drawLine(268 + (177 / 3 * i), 370, 268 + (177 / 3 * i), 547);
			g2d.drawLine(268, 370 + (177 / 3 * i), 445, 370 + (177 / 3 * i));
		}

		g2d.setFont(FONT.deriveFont(Font.PLAIN, 50));
		g2d.setColor(Color.WHITE);
		printCenteredString("LEVEL", 196, 52, 440, g2d);
		String name = m.getEffectiveName();
		if (g2d.getFontMetrics().stringWidth(m.getEffectiveName()) >= 678)
			name = m.getEffectiveName().substring(0, 21).concat("...");
		drawOutlinedText(name, 270, 342, g2d);

		if (!Member.getWaifu(m.getUser()).isEmpty()) {
			g2d.setFont(FONT.deriveFont(Font.PLAIN, 30));
			drawOutlinedText("Casado(a) com: " + Main.getInfo().getUserByID(Member.getWaifu(m.getUser())).getName(), 270, 298, g2d);
		}

		g2d.setFont(FONT.deriveFont(Font.BOLD, 85));
		printCenteredString(String.valueOf(mb.getLevel()), 196, 52, 515, g2d);

		g2d.setFont(FONT.deriveFont(Font.PLAIN, 25));
		printCenteredString(mb.getXp() + "/" + ((int) Math.pow(mb.getLevel(), 2) * 100), 196, 52, 538, g2d);

		List<Member> mbs = MemberDAO.getMemberRank(g.getId(), false);
		int pos = 0;
		for (int i = 0; i < mbs.size(); i++) {
			if (mbs.get(i).getId().equals(m.getUser().getId() + g.getId())) {
				pos = i + 1;
				break;
			}
		}

		mbs = MemberDAO.getMemberRank(g.getId(), true);
		int posG = 0;
		for (int i = 0; i < mbs.size(); i++) {
			if (mbs.get(i).getId().equals(m.getUser().getId() + g.getId())) {
				posG = i + 1;
				break;
			}
		}

		g2d.setFont(FONT.deriveFont(Font.PLAIN, 40));
		g2d.setColor(Color.WHITE);
		printCenteredString("Emblemas", 182, 266, 590, g2d);
		printCenteredString("Biografia", 460, 466, 590, g2d);

		g2d.setFont(FONT.deriveFont(Font.PLAIN, 30));
		printCenteredString("Rank: #" + pos + "/#" + posG, 196, 52, 585, g2d);


		g2d.setFont(new Font("DejaVu Sans", Font.BOLD, 25));
		String s = mb.getBio();
		drawStringMultiLine(g2d, s.isEmpty() ? "Sem biografia" : s, 440, 474, 403);

		drawBadges(m, mb, g, g2d);

		g2d.setClip(new Ellipse2D.Float(50, 200, avatar.getWidth(), avatar.getHeight()));
		g2d.fillOval(50, 200, avatar.getWidth(), avatar.getHeight());
		g2d.drawImage(avatar, null, 50, 200);

		g2d.dispose();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(clipRoundEdges(bi), "png", baos);

		return baos;
	}

	public static BufferedImage clipRoundEdges(BufferedImage image) {
		BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();

		g2d.setClip(new RoundRectangle2D.Float(0, 0, bi.getWidth(), bi.getHeight(), 80, 80));
		g2d.drawImage(image, null, 0, 0);
		g2d.dispose();

		return bi;
	}

	private static void drawBadges(net.dv8tion.jda.api.entities.Member m, Member mb, Guild s, Graphics2D g2d) throws IOException {
		List<BufferedImage> badges = new ArrayList<>() {{
			String exceed = ExceedDAO.getExceed(m.getId());
			if (!exceed.isEmpty()) {
				add(ImageIO.read(Helper.getImage(Objects.requireNonNull(Main.getInfo().getAPI().getEmoteById(TagIcons.getExceedId(ExceedEnums.getByName(exceed)))).getImageUrl())));
			}

			Set<Tag> tags = Tag.getTags(m.getUser(), m);
			tags.forEach(t -> {
				try {
					add(ImageIO.read(t.getPath(mb)));
				} catch (IOException e) {
					Helper.logger(Profile.class).error(e + " | " + e.getStackTrace()[0]);
				} catch (NullPointerException ignore) {
				}
			});
		}};

		List<int[]> coords = new ArrayList<>() {{
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					add(new int[]{276 + (59 * x), 378 + (59 * y)});
				}
			}
		}};

		for (int i = 0; i < badges.size(); i++) {
			g2d.drawImage(Helper.scaleImage(badges.get(i), 44, 44), null, coords.get(i)[0], coords.get(i)[1]);
		}
	}

	public static void printCenteredString(String s, int width, int XPos, int YPos, Graphics2D g2d) {
		int stringLen = g2d.getFontMetrics().stringWidth(s);
		int start = width / 2 - stringLen / 2;
		drawOutlinedText(s, start + XPos, YPos, g2d);
	}

	private static void drawOutlinedText(String s, int x, int y, Graphics2D g2d) {
		AffineTransform transform = g2d.getTransform();
		transform.translate(x, y);
		g2d.transform(transform);
		makeOutline(s, g2d);
		transform.translate(-x, -y);
		g2d.setTransform(transform);
	}

	private static void makeOutline(String s, Graphics2D g2d) {
		Color c = g2d.getColor();
		s = s.isEmpty() ? "Não definido" : s;
		g2d.setColor(g2d.getBackground());
		FontRenderContext frc = g2d.getFontRenderContext();
		TextLayout tl = new TextLayout(s, g2d.getFont(), frc);
		Shape shape = tl.getOutline(null);
		g2d.setStroke(new BasicStroke(4));
		g2d.draw(shape);
		g2d.setColor(c);
		g2d.fill(shape);
	}

	public static void drawStringMultiLineNO(Graphics2D g, String text, int lineWidth, int x, int y) {
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

	public static void drawStringMultiLine(Graphics2D g, String text, int lineWidth, int x, int y) {
		FontMetrics m = g.getFontMetrics();
		if (m.stringWidth(text) < lineWidth) {
			drawOutlinedText(text, x, y, g);
		} else {
			String[] words = text.split(" ");
			StringBuilder currentLine = new StringBuilder(words[0]);
			for (int i = 1; i < words.length; i++) {
				if (m.stringWidth(currentLine + words[i]) < lineWidth) {
					currentLine.append(" ").append(words[i]);
				} else {
					String s = currentLine.toString();
					drawOutlinedText(s, x, y, g);
					y += m.getHeight();
					currentLine = new StringBuilder(words[i]);
				}
			}
			if (currentLine.toString().trim().length() > 0) {
				drawOutlinedText(currentLine.toString(), x, y, g);
			}
		}
	}

	public static void drawRotate(Graphics2D g2d, double x, double y, int angle, String text) {
		g2d.translate((float) x, (float) y);
		g2d.rotate(Math.toRadians(angle));
		makeOutline(text, g2d);
		g2d.rotate(-Math.toRadians(angle));
		g2d.translate(-(float) x, -(float) y);
	}
}
