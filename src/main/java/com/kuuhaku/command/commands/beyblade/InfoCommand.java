package com.kuuhaku.command.commands.beyblade;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Beyblade;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.awt.*;
import java.util.Objects;

public class InfoCommand extends Command {

    public InfoCommand() {
        super("binfo", new String[]{"bbeyblade", "bb"}, "Mostra dados sobre sua Beyblade.", Category.BEYBLADE);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (MySQL.getBeybladeById(author.getId()) == null) {
            channel.sendMessage(":x: | Você não possui uma Beyblade.").queue();
            return;
        }

        channel.sendMessage("<a:Loading:598500653215645697> Analizando...").queue(m -> {
            Beyblade bb = Objects.requireNonNull(MySQL.getBeybladeById(author.getId()));
            EmbedBuilder eb = new EmbedBuilder();

            if (args.length == 1 && args[0].equals("casa")) {
                if (bb.getS() == null) {
                    m.editMessage(":x: | Você não possui um alinhamento.").queue();
                }
                eb.setAuthor("Alinhamento de " + bb.getName() + ":");
                switch (bb.getS().getType()) {
                    case "TIGER":
                        eb.setThumbnail("https://i.imgur.com/fLZPBP8.png");
                        break;
                    case "DRAGON":
                        eb.setThumbnail("https://i.imgur.com/g2L0cfy.png");
                        break;
                    case "BEAR":
                        eb.setThumbnail("https://i.imgur.com/MG789l8.png");
                        break;
                }
                eb.setColor(Color.decode(bb.getColor()));
                eb.addField(bb.getS().getName(), bb.getS().getDescription(), false);
                eb.addField("Dificuldade:", Integer.toString(bb.getS().getDiff()), true);

                m.delete().queue();
                channel.sendMessage(eb.build()).queue();
                return;
            }

            eb.setAuthor("Beyblade de " + message.getAuthor().getName(), message.getAuthor().getAvatarUrl());
            eb.setTitle(bb.getName());
            eb.setColor(Color.decode(bb.getColor()));
            if (bb.getS() == null) eb.setThumbnail("https://www.beybladetr.com/img/BeybladeLogolar/BeyIcon.png");
            else if (bb.getS().getType().equals("TIGER")) eb.setThumbnail("https://i.imgur.com/fLZPBP8.png");
            else if (bb.getS().getType().equals("DRAGON")) eb.setThumbnail("https://i.imgur.com/g2L0cfy.png");
            else if (bb.getS().getType().equals("BEAR")) eb.setThumbnail("https://i.imgur.com/MG789l8.png");
            eb.addField("Velocidade:", Float.toString(bb.getSpeed()), true);
            eb.addField("Força:", Float.toString(bb.getStrength()), true);
            eb.addField("Estabilidade:", Float.toString(bb.getStability()), true);
            eb.addField("Especial:", bb.getS() != null ? "(" + bb.getSpecial() + ")" + bb.getS().getName() : "Não obtido", true);
            eb.addField("Vitórias/Derrotas:", bb.getWins() + "/" + bb.getLoses(), true);
            eb.addField(":diamond_shape_with_a_dot_inside: Pontos de combate:", bb.getPoints() + " pontos", true);

            m.delete().queue();
            channel.sendMessage(eb.build()).queue();
        });
    }
}
