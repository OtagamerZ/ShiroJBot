package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.Profile;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.IOException;

public class ProfileCommand extends Command {

    public ProfileCommand() {
        super("perfil", new String[]{"xp", "profile", "pf"}, "Mostra dados sobre você neste servidor.", Category.INFO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        channel.sendMessage(":hourglass_flowing_sand: Gerando perfil...").queue(m -> {
            try {
                m.delete().queue();
                channel.sendMessage(":video_game: Perfil de " + author.getAsMention()).addFile(Profile.makeProfile(message.getTextChannel(), member, guild).toByteArray(), "perfil.png").queue();
            } catch (IOException e) {
                m.editMessage(":x: | Epa, teve um errinho aqui enquanto eu gerava o perfil, meus criadores já foram notificados!").queue();
                Helper.log(this.getClass(), LogLevel.ERROR, e.toString());
            }
        });
    }
}
