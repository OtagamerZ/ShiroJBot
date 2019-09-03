package com.kuuhaku.command.commands.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
            StringBuilder sb = new StringBuilder();
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

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(clipRoundEdges(bi), "png", baos);

            String n1 = message.getMentionedUsers().get(0).getName();
            String n2 = message.getMentionedUsers().get(1).getName();

            sb.append(":heartpulse: ***Nível de love entre ").append(message.getMentionedUsers().get(0).getName()).append(" e ").append(message.getMentionedUsers().get(1).getName()).append(":***");
            sb.append("\n\nNome de casal: `").append(n1, 0, n1.length() / 2 + (n1.length() % 2)).append(n2.substring(n2.length() / 2 - (n1.length() % 2))).append("`");
            if (love <= 30)
                sb.append("\n\nBem, esse casal jamais daria certo, hora de passar pra frente!\n**").append(Helper.round(love, 1)).append("%** `").append(doneMeter).append("`");
            else if (love <= 50)
                sb.append("\n\nPode ate dar certo esse casal, mas vai precisar insistir!\n**").append(Helper.round(love, 1)).append("%** `").append(doneMeter).append("`");
            else if (love <= 70)
                sb.append("\n\nOpa, ou eles já se conhecem, ou o destino sorriu pra eles!\n**").append(Helper.round(love, 1)).append("%** `").append(doneMeter).append("`");
            else
                sb.append("\n\nImpossível casal mais perfeito que esse, tem que casar JÁ!!\n**").append(Helper.round(love, 1)).append("%** `").append(doneMeter).append("`");

            EmbedBuilder eb = new EmbedBuilder();
            eb.setImage("attachment://ship.png");
            eb.setColor(Helper.getRandomColor());

            MessageBuilder mb = new MessageBuilder();
            mb.append(sb.toString());
            mb.setEmbed(eb.build());

            channel.sendMessage(mb.build()).addFile(baos.toByteArray(), "ship.png").queue();
        } catch (IOException e) {
            Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
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
