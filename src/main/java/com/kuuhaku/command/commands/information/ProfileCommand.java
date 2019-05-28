package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.ProfileTest;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.awt.*;
import java.io.IOException;

public class ProfileCommand extends Command {

    public ProfileCommand() {
        super("perfil", new String[] {"xp", "profile", "pf"}, "Mostra dados sobre você neste servidor.", Category.INFO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        try {
            channel.sendFile(new ProfileTest().makeProfile(member), "perfil.jpg").queue();
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
    }
}
