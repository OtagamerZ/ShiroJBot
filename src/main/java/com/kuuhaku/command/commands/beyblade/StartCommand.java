package com.kuuhaku.command.commands.beyblade;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Beyblade;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class StartCommand extends Command {

    public StartCommand() {
        super("bstart", new String[]{"bcomeçar", "pegarbeyblade"}, "<nome>", "Inicia seu perfil de Beyblader.", Category.BEYBLADE);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (MySQL.getBeybladeById(author.getId()) != null) {
            channel.sendMessage(":x: | Você já possui uma Beyblade.").queue();
            return;
        } else if (args.length == 0) {
            channel.sendMessage(":x: | Você precisa dar um nome a sua Beyblade.").queue();
            return;
        }

        String txt = String.join(" ", args);

        if (txt.length() > 16) {
            channel.sendMessage(":x: | Você escolheu um nome muito longo.").queue();
            return;
        }

        channel.sendMessage("<a:Loading:598500653215645697> Gerando dados...").queue(m -> {
            Beyblade bb = new Beyblade();
            bb.setId(author.getId());
            bb.setName(txt);
            MySQL.sendBeybladeToDB(bb);
            m.editMessage("Você pegou sua Beyblade, dêem as boas vindas para " + bb.getName() + "!").queue();
        });
    }
}
