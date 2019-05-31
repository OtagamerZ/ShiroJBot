package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Settings;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.IOException;

public class SettingsCommand extends Command {

    public SettingsCommand() {
        super("settings", new String[]{"setting", "definições", "definiçoes", "definicões", "parametros", "parâmetros"}, "<parâmetro> <novo valor do parâmetro>", "Muda as configurações da Shiro no seu servidor.", Category.MODERACAO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

        guildConfig gc = SQLite.getGuildById(guild.getId());

        if (args.length == 0) {
            try {
                Settings.embedConfig(message);
            } catch (IOException err) {
                channel.sendMessage(":x: | Ocorreu um erro durante o processo, os meus developers já foram notificados.").queue();
                err.printStackTrace();
                return;
            }

            return;
        }

        switch (args[0].toLowerCase()) {
            case "prefix":
            case "prefixo":
                Settings.updatePrefix(args, message, gc);
                break;
            case "cbv":
            case "canalbv":
                Settings.updateCanalBV(args, message, gc);
                break;
            case "cadeus":
            case "canaladeus":
                Settings.updateCanalAdeus(args, message, gc);
                break;
            case "csug":
            case "canalsug":
                Settings.updateCanalSUG(args, message, gc);
                break;
            case "cavisos":
            case "canalavisos":
                Settings.updateCanalAvisos(args, message, gc);
                break;
            case "rwarn":
            case "rolewarn":
            case "cwarn":
            case "cargowarn":
            case "raviso":
            case "roleaviso":
            case "caviso":
            case "cargoaviso":
                Settings.updateCargoWarn(args, message, gc);
                break;
//            case "":
//            case "":
            default:
                try {
                    Settings.embedConfig(message);
                } catch (IOException err) {
                    channel.sendMessage(":x: | Ocorreu um erro durante o processo, os meus developers já foram notificados.").queue();
                    err.printStackTrace();
                }
        }
    }
}
