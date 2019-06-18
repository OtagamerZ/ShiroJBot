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

import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import com.kuuhaku.utils.PrivilegeLevel;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class Profile {
    public InputStream makeProfile(net.dv8tion.jda.core.entities.Member u) throws IOException, FontFormatException {
        Member m = SQLite.getMemberById(u.getUser().getId() + u.getGuild().getId());
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("font/friz-quadrata-bold-bt.ttf"))));

        try {
            MySQL.getTagById(u.getUser().getId());
        } catch (NoResultException e) {
            MySQL.addUserTagsToDB(u);
        }

        BufferedImage profile = new BufferedImage(1055, 719, BufferedImage.TYPE_INT_RGB);

        HttpURLConnection con = (HttpURLConnection) new URL(m.getBg()).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedImage bg = scaleImage(ImageIO.read(con.getInputStream()));
        Helper.log(this.getClass(), LogLevel.DEBUG, bg.getWidth() + "x" + bg.getHeight());

        con = (HttpURLConnection) new URL("http://i.imgur.com/gRzI7PH.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage vignette = ImageIO.read(con.getInputStream());

        con = (HttpURLConnection) new URL(u.getUser().getAvatarUrl()).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage avatar = resize(ImageIO.read(con.getInputStream()), 120, 120);

        con = (HttpURLConnection) new URL(MySQL.getTagById(u.getUser().getId()).isPartner() ? "http://i.imgur.com/rbNA1Mi.png" : "http://i.imgur.com/373xhkZ.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage banner = ImageIO.read(con.getInputStream());

        if (Helper.hasPermission(u, PrivilegeLevel.NIICHAN)) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Challenger_Trim.png").openConnection();
        } else if (Helper.hasPermission(u, PrivilegeLevel.DEV)) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Grandmaster_Trim.png").openConnection();
        } else if (Helper.hasPermission(u, PrivilegeLevel.MOD)) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Master_Trim.png").openConnection();
        } else if (m.getLevel() >= 30) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Diamond_Trim.png").openConnection();
        } else if (m.getLevel() >= 25) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Platinum_Trim.png").openConnection();
        } else if (m.getLevel() >= 20) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Gold_Trim.png").openConnection();
        } else if (m.getLevel() >= 15) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Silver_Trim.png").openConnection();
        } else if (m.getLevel() >= 10) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Bronze_Trim.png").openConnection();
        } else if (m.getLevel() >= 5) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Iron_Trim.png").openConnection();
        } else if (Helper.hasPermission(u, PrivilegeLevel.USER)) {
            con = (HttpURLConnection) new URL("http://img.rankedboost.com/wp-content/uploads/2016/06/Season_2019_-_Default_Trim.png").openConnection();
        }
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedImage bannerBorder = resize(ImageIO.read(con.getInputStream()), 216, 108);

        con = (HttpURLConnection) new URL("http://i.imgur.com/rxZ5qAL.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage header = ImageIO.read(con.getInputStream());

        con = (HttpURLConnection) new URL("http://i.imgur.com/FhOanld.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage level = ImageIO.read(con.getInputStream());

        con = (HttpURLConnection) new URL("http://i.imgur.com/tcauuXO.png").openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage search = ImageIO.read(con.getInputStream());

        con = (HttpURLConnection) new URL(getLevel(m.getLevel())).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage lvlBorder = resize(ImageIO.read(con.getInputStream()), 217, 217);

        Graphics2D g2d = profile.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(bg, null, 0 - (bg.getWidth() - 1055) / 2, 0);
        g2d.drawImage(vignette, null, 0, 0);
        g2d.drawImage(avatar, null, 93, 283);
        g2d.drawImage(banner, null, MySQL.getTagById(u.getUser().getId()).isPartner() ? 1 : 45, 0);
        g2d.drawImage(bannerBorder, null, 45, 498);
        g2d.drawImage(header, null, 0, 0);

        GradientPaint levelPaint = new GradientPaint(104, 210, Color.decode("#0e628d"), 241, 210, Color.decode("#0cadae"));
        g2d.setPaint(levelPaint);
        g2d.fillRect(104, 210, Math.round(m.getXp() * 137 / (int) Math.pow(m.getLevel(), 2)) / 100, 7);

        g2d.drawImage(level, null, 53, 197);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("FrizQuadrata BT", Font.PLAIN, 12));
        printCenteredString(Integer.toString(m.getLevel()), 26, 71, 218, g2d);

        g2d.drawImage(search, null, 786, 95);
        g2d.drawImage(lvlBorder, null, 44, 234);
        printCenteredString(Integer.toString(m.getLevel()), 32, 138, 412, g2d);

        String tempName = u.getEffectiveName();
        g2d.setFont(new Font("FrizQuadrata BT", Font.PLAIN, 30));
        if ((int) g2d.getFontMetrics().getStringBounds(tempName, g2d).getWidth() >= 213)
            tempName = tempName.substring(0, 6).concat("...");
        printCenteredString(tempName, 213, 47, 166, g2d);

        if (MySQL.getTagById(u.getUser().getId()).isToxic()) {
            con = (HttpURLConnection) new URL("http://i.imgur.com/0wCblVy.png").openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            final BufferedImage toxic = ImageIO.read(con.getInputStream());
            g2d.drawImage(toxic, null, 0, 0);
        }

        g2d.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(profile, "jpg", baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private static String getLevel(int lvl) {
        if (lvl < 10) {
            return "https://i.imgur.com/Noji8Tc.png";
        } else if (lvl < 20) {
            return "https://i.imgur.com/zfFBzI5.png";
        } else if (lvl < 30) {
            return "https://i.imgur.com/bSldQTJ.png";
        } else {
            return "https://i.imgur.com/C56zq8q.png";
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

    private static BufferedImage scaleImage(BufferedImage image) {
        int newWidth = 1055;
        int newHeight = 719;

        // Make sure the aspect ratio is maintained, so the image is not distorted
        double thumbRatio = (double) newWidth / (double) newHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double aspectRatio = (double) imageWidth / (double) imageHeight;

        if (thumbRatio > aspectRatio) {
            newHeight = (int) (newWidth / aspectRatio);
        } else {
            newWidth = (int) (newHeight * aspectRatio);
        }

        // Draw the scaled image
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, newWidth, newHeight, null);

        return newImage;
    }

    private static void printCenteredString(String s, int width, int XPos, int YPos, Graphics2D g2d) {
        int stringLen = (int) g2d.getFontMetrics().getStringBounds(s, g2d).getWidth();
        int start = width / 2 - stringLen / 2;
        g2d.drawString(s, start + XPos, YPos);
    }
}
