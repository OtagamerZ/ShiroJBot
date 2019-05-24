package com.kuuhaku.model;

import com.kuuhaku.controller.Database;
import net.dv8tion.jda.core.entities.Member;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class ProfileTest {
    public InputStream makeProfile(Member u) throws IOException, FontFormatException {
        com.kuuhaku.model.Member m = Database.getMemberById(u.getUser().getId() + u.getGuild().getId());
        final Font font = Font.createFont(Font.TRUETYPE_FONT, this.getClass().getResourceAsStream("/resources/font/friz-quadrata-bold-bt.ttf"));

        BufferedImage profile = new BufferedImage(1055, 719, BufferedImage.TYPE_INT_RGB);

        HttpURLConnection con = (HttpURLConnection) new URL("https://i.imgur.com/S3me8Oj.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage vignette = ImageIO.read(con.getInputStream());

        con = (HttpURLConnection) new URL(u.getUser().getAvatarUrl()).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage avatar = resize(ImageIO.read(con.getInputStream()), 120, 120);

        con = (HttpURLConnection) new URL("https://i.imgur.com/373xhkZ.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage banner = ImageIO.read(con.getInputStream());

        con = (HttpURLConnection) new URL("https://i.imgur.com/rxZ5qAL.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage header = ImageIO.read(con.getInputStream());

        con = (HttpURLConnection) new URL("https://i.imgur.com/FhOanld.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage level = ImageIO.read(con.getInputStream());

        con = (HttpURLConnection) new URL("https://i.imgur.com/tcauuXO.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage search = ImageIO.read(con.getInputStream());

        con = (HttpURLConnection) new URL(getLevel(m.getLevel())).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage lvlBorder = resize(ImageIO.read(con.getInputStream()), 217, 217);

        Graphics2D g2d = profile.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(vignette, null, 0, 0);
        g2d.drawImage(avatar, null, 93, 283);
        g2d.drawImage(banner, null, 45, 0);
        g2d.drawImage(header, null, 0, 0);
        GradientPaint levelPaint = new GradientPaint(104, 210, Color.decode("#0e628d"), 241, 210, Color.decode("#0cadae"));

        g2d.setPaint(levelPaint);
        g2d.fillRect(104, 210, Math.round(m.getXp() * 137 / (int) Math.pow(m.getLevel(), 2)) / 100, 7);

        g2d.drawImage(level, null, 53, 197);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(font.getName(), Font.PLAIN, 12));
        printCenteredString(Integer.toString(m.getLevel()), 27, 71, 216, g2d);

        g2d.drawImage(search, null, 786, 95);
        g2d.drawImage(lvlBorder, null, 44, 234);
        printCenteredString(Integer.toString(m.getLevel()), 31, 138, 411, g2d);

        if (Objects.requireNonNull(Database.getTags()).get(u.getUser().getId()).isStaff()) {
            con = (HttpURLConnection) new URL("https://i.imgur.com/YByt8rb.png").openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            final BufferedImage admin = resize(ImageIO.read(con.getInputStream()), 116, 132);
            g2d.drawImage(admin, null, 601, 547);
        }

        if (Database.getTags().get(u.getUser().getId()).isToxic()) {
            con = (HttpURLConnection) new URL("https://images-wixmp-ed30a86b8c4ca887773594c2.wixmp.com/f/c9b92027-9722-4fe8-98fa-c288aeead3d9/d64d9s8-6e1d37c9-5253-4b5a-9a80-be52c3b3b81a.png/v1/fill/w_774,h_1033,strp/warning__toxic_teemo_shrooms__by_luciedesigns_d64d9s8-pre.png").openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            final BufferedImage honor = resize(ImageIO.read(con.getInputStream()), 101, 135);
            g2d.drawImage(honor, null, 748, 564);
        }

        if (Database.getTags().get(u.getUser().getId()).isPartner()) {
            con = (HttpURLConnection) new URL("https://i.imgur.com/HMm7gHp.png").openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            final BufferedImage partner = resize(ImageIO.read(con.getInputStream()), 95, 124);
            g2d.drawImage(partner, null, 901, 966);
        }

        g2d.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(profile, "jpg", baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private static String getLevel(int lvl) {
        if (lvl < 30) {
            return "http://www.sclance.com/pngs/metal-border-png/metal_border_png_864177.png?width=217&height=217";
        } else if (lvl < 50) {
            return "https://vignette.wikia.nocookie.net/leagueoflegends/images/4/40/Level_30_Summoner_Icon_Border.png";
        } else if (lvl < 75) {
            return "https://vignette.wikia.nocookie.net/leagueoflegends/images/c/c0/Level_50_Summoner_Icon_Border.png";
        } else {
            return "https://vignette.wikia.nocookie.net/leagueoflegends/images/d/d7/Level_75_Summoner_Icon_Border.png";
        }
    }

    private static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    private static void printCenteredString(String s, int width, int XPos, int YPos, Graphics2D g2d) {
        int stringLen = (int)
                g2d.getFontMetrics().getStringBounds(s, g2d).getWidth();
        int start = width / 2 - stringLen / 2;
        g2d.drawString(s, start + XPos, YPos);
    }
}
