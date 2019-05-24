package com.kuuhaku.model;

public class ProfileTest {
    private InputStream makeProfile(Member u) throws IOException, FontFormatException {
        final Font font = Font.createFont(Font.TRUETYPE_FONT, this.getClass().getResourceAsStream("font/friz-quadrata-bold-bt.ttf"));

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

        con = (HttpURLConnection) new URL(getLevel(5)).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedImage lvlBorder = resize(ImageIO.read(con.getInputStream()), 217, 217);

        Graphics2D g2d = profile.createGraphics();
        g2d.drawImage(vignette, null, 0, 0);
        g2d.drawImage(avatar, null, 93, 283);
        g2d.drawImage(banner, null, 45, 0);
        g2d.drawImage(header, null, 0, 0);
        GradientPaint levelPaint = new GradientPaint(104, 210, Color.decode("#0e628d"), 241, 210, Color.decode("#0cadae"));

        g2d.setPaint(levelPaint);
        g2d.fillRect(104, 210, 137, 7);

        g2d.drawImage(level, null, 53, 197);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(font.getName(), Font.PLAIN, 12));
        g2d.drawString("13", 93 / "13".length(), 218);

        g2d.drawImage(search, null, 786, 95);
        g2d.drawImage(lvlBorder, null, 44, 234);
        g2d.drawString("13", 138 + (170 / "13".length()), 410);

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

    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }
}
