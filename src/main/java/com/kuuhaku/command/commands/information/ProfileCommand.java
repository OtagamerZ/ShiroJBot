package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.Profile;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.awt.*;
import java.io.IOException;

public class ProfileCommand extends Command {

    public ProfileCommand() {
        super("perfil", new String[]{"xp", "profile", "pf"}, "Mostra dados sobre você neste servidor.", Category.INFO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        channel.sendMessage(":hourglass_flowing_sand: Gerando perfil...").queue(m -> {
            try {
                channel.sendFile(new Profile().makeProfile(member), "perfil.jpg").queue(f -> m.editMessage(":video_game: Perfil de " + author.getAsMention()).queue());
            } catch (IOException | FontFormatException e) {
                m.editMessage(":x: | Epa, teve um errinho aqui enquanto eu gerava o perfil, meus criadores já foram notificados!").queue();
                e.printStackTrace();
            }
        });
    }
}
