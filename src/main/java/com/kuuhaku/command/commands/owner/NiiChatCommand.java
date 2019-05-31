package com.kuuhaku.command.commands.owner;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class NiiChatCommand extends Command {

    public NiiChatCommand() {
        super("chat", new String[]{"duowithme"}, "Alterna com a Shiro em quem fala!", Category.NIICHAN);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (Main.getInfo().isNiimode()) {
            Main.getInfo().switchNiichat();
            channel.sendMessage("Modo chat " + (Main.getInfo().isNiichat() ? "ligado!" : "desligado!")).queue();
        }
    }
}
