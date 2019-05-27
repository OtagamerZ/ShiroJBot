package com.rdx.command.commands.fun;

import com.kuuhaku.model.ReactionsList;
import com.rdx.Main;
import com.rdx.command.Category;
import com.rdx.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

public class TapaCommand extends Command {

    private static final boolean answer = false;

    public TapaCommand() {
        super("tapa", new String[]{}, "<@usuário>", "Dá um tapa na pessoa mencionada.", Category.FUN);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

        if (message.getMentionedUsers().size() == 1) {
            if (!message.getMentionedUsers().get(0).getId().equals(author.getId())) {
                try {
                    URL url = new URL(ReactionsList.hug()[new Random().nextInt(ReactionsList.hug().length)]);
                    HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
                    con.setRequestProperty("User-Agent", "Mozilla/5.0");

                    String msg = "";
                    switch (new Random().nextInt(3)) {
                        case 0:
                            msg = "Kono BAKAAAA!!";
                            break;
                        case 1:
                            msg = "Calma, deixa ele(a) respirar!";
                            break;
                        case 2:
                            msg = "Toma essa!";
                            break;
                    }

                    if (message.getMentionedUsers().get(0).getId().equals(Main.getInfo().getSelfUser().getId())) {
                        if (message.getAuthor().getId().equals("350836145921327115")) {
                            switch (new Random().nextInt(3)) {
                                case 0:
                                    msg = ("Nii-chan no BAKA!");
                                    break;
                                case 1:
                                    msg = ("Ai, vai mesmo bater na sua Nee-chan?");
                                    break;
                                case 2:
                                    msg = ("EU TO JOGANDO!");
                                    break;
                            }
                            channel.sendMessage(msg).addFile(con.getInputStream(), "slap.gif").queue();
                        } else {
                            channel.sendMessage(msg).addFile(con.getInputStream(), "slap.gif").queue();
                        }
                    } else {
                        if (!answer) {
                            msg = (author.getAsMention() + " deu um tapa em " + message.getMentionedUsers().get(0).getAsMention() + " - " + msg);
                            channel.sendMessage(msg).addFile(con.getInputStream(), "slap.gif").queue(m -> m.addReaction("\u21aa").queue());
                        } else {
                            msg = (message.getMentionedUsers().get(0).getAsMention() + " respondeu o tapa de " + author.getAsMention() + " - " + msg);
                            channel.sendMessage(msg).addFile(con.getInputStream(), "slap.gif").queue();
                        }
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            } else {
                channel.sendMessage(":x: | Você não pode dar um tapa em si mesmo...").queue();
            }
        } else if (message.getMentionedUsers().size() > 1) {
            channel.sendMessage(":x: | Você só pode dar um tapa em uma pessoa de cada vez!").queue();
        } else {
            channel.sendMessage(":x: | Você tem que mencionar a pessoa que quer dar um tapa!").queue();
        }
    }
}
