package com.rdx.command.commands.fun;

import com.kuuhaku.model.ReactionsList;
import com.rdx.command.Category;
import com.rdx.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

public class NopeCommand extends Command {

    public NopeCommand() {
        super("nope", new String[]{"sqn", "não"}, "<@usuário>", "Esquiva-se de uma tentativa.", Category.FUN);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        try {
            URL url = new URL(ReactionsList.nope()[new Random().nextInt(ReactionsList.nope().length)]);
            HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            String msg = "";
            switch (new Random().nextInt(3)) {
                case 0:
                    msg = "Não tão rápido!";
                    break;
                case 1:
                    msg = "Só que não!";
                    break;
                case 2:
                    msg = "Hoje não!";
                    break;
            }

            msg = (author.getAsMention() + " esquivou-se - " + msg);
            message.getChannel().sendMessage(msg).addFile(con.getInputStream(), "nope.gif").queue();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}

