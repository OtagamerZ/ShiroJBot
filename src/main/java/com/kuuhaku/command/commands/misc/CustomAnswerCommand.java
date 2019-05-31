package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class CustomAnswerCommand extends Command {

    public CustomAnswerCommand() {
        super("fale", "<gatilho>;<resposta>", "Configura uma resposta para o gatilho (frase) especificado.", Category.MISC);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (!Helper.hasPermission(member, PrivilegeLevel.STAFF) && !SQLite.getGuildById(guild.getId()).isAnyTell()) {
            channel.sendMessage(":x: | Este servidor não está configurado para permitir respostas customizadas da comunidade.").queue();
            return;
        } else if (args.length == 0) {
            channel.sendMessage(":x: | Você precisa definir um gatilho e uma mensagem.").queue();
            return;
        }

        String txt = String.join(" ", args);

        if (txt.contains(";")) {
            SQLite.addCAtoDB(guild, txt.split(";")[0], txt.split(";")[1]);
            channel.sendMessage("Agora quando alguém disser `" + txt.split(";")[0] + "` irei responder `" + txt.split(";")[1] + "`.").queue();
        } else {
            channel.sendMessage(":x: | O gatilho e a resposta devem estar separados por ponto e virgula (`;`).").queue();
        }
    }
}
