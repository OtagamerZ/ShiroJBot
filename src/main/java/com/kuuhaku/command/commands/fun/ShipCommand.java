package com.kuuhaku.command.commands.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class ShipCommand extends Command {

    public ShipCommand() {
        super("ship", new String[]{"shippar"}, "<usuário 1> <usuário 2>", "Mede o nível de love entre duas pessoas.", Category.FUN);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (message.getMentionedUsers().size() < 2) {
            channel.sendMessage(":x: | Você precisa mencionar dois usuários!").queue();
            return;
        }

        try {
            EmbedBuilder eb = new EmbedBuilder();
            String[] meter = {"-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-"};
            String doneMeter;
            BufferedImage bi = new BufferedImage(257, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bi.createGraphics();
            float love = 100 * new Random(message.getMentionedUsers().get(0).getIdLong() + message.getMentionedUsers().get(1).getIdLong()).nextFloat();

            for (int i = 0; i < Math.round(love / 5); i++) {
                meter[i] = "▉";
            }

            doneMeter = Arrays.toString(meter).replace(",", "").replace(" ", "");

            g2d.drawImage(ImageIO.read(Helper.getImage(message.getMentionedUsers().get(0).getAvatarUrl())), null, 0, 0);
            g2d.drawImage(ImageIO.read(Helper.getImage(message.getMentionedUsers().get(1).getAvatarUrl())), null, 129, 0);

            g2d.dispose();

            File f = new File("ship.png");
            ImageIO.write(clipRoundEdges(bi), "png", f);

            eb.setTitle(":heartpulse: Nível de love entre " + message.getMentionedUsers().get(0).getName() + " e " + message.getMentionedUsers().get(0).getName() + ":");
            if (love <= 30)
                eb.setDescription("Bem, esse casal jamais daria certo, hora de passar pra frente!\n` " + Helper.round(love, 1) + "% " + doneMeter + "`");
            else if (love <= 50)
                eb.setDescription("Pode ate dar certo esse canal, mas vai precisar insistir!\n` " + Helper.round(love, 1) + "% " + doneMeter + "`");
            else if (love <= 70)
                eb.setDescription("Opa, ou eles já se conhecem, ou o destino sorriu pra eles!\n` " + Helper.round(love, 1) + "% " + doneMeter + "`");
            else
                eb.setDescription("Impossível casal mais perfeito que esse, tem que casar JÁ!!\n` " + Helper.round(love, 1) + "% " + doneMeter + "`");

            eb.setImage(f.toURI().toURL().toString());

            channel.sendMessage(eb.build()).queue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage clipRoundEdges(BufferedImage image) {
        BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setClip(new RoundRectangle2D.Float(0, 0, bi.getWidth(), bi.getHeight(), 20, 20));
        g2d.drawImage(image, null, 0, 0);
        g2d.dispose();

        return bi;
    }
}
