package com.kuuhaku.command.commands.beyblade;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Beyblade;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.util.Objects;

public class ColorCommand extends Command {

    public ColorCommand() {
        super("bcolor", new String[]{"bcor", "mudarcor"}, "<#rrbbgg>", "Muda a cor da Beyblade.", Category.BEYBLADE);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (MySQL.getBeybladeById(author.getId()) == null) {
            channel.sendMessage(":x: | Você não possui uma Beyblade.").queue();
            return;
        } else if (args.length == 0) {
            channel.sendMessage(":x: | Você precisa especificar uma cor.").queue();
            return;
        } else if (args[0].length() != 7 && !args[0].contains("#")) {
            channel.sendMessage(":x: | Cor inválida, o formato deve ser `#rrggbb`.").queue();
            return;
        }

        channel.sendMessage("<a:Loading:598500653215645697> Analizando...").queue(m -> {
            Beyblade bb = MySQL.getBeybladeById(author.getId());
            Objects.requireNonNull(bb).setColor(args[0]);
            MySQL.sendBeybladeToDB(bb);
            m.editMessage("Cor trocada com sucesso!").queue();
        });
    }
}
