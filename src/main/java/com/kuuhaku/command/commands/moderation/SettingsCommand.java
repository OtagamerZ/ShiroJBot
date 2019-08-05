package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
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
                Helper.log(this.getClass(), LogLevel.ERROR, err + " | " + err.getStackTrace()[0]);
                return;
            }

            return;
        }

        final String msg = String.join(" ", args).replace(args[0], "").replace(args[1], "").trim();

        switch (args[0].toLowerCase()) {
            case "prefix":
            case "prefixo":
                if (msg.length() > 5) {
                    channel.sendMessage(":x: | Prefixo muito longo (Max. 5)").queue();
                    return;
                }
                Settings.updatePrefix(args, message, gc);
                break;
            case "cbv":
            case "canalbv":
                Settings.updateCanalBV(args, message, gc);
                break;
            case "mensagembemvindo":
            case "mensagembv":
            case "msgbv":
                if (msg.length() > 2000) {
                    channel.sendMessage(":x: | Mensagem muito longo (Max. 2000)").queue();
                    return;
                }
                Settings.updateMsgBV(args, message, gc);
                break;
            case "cadeus":
            case "canaladeus":
                Settings.updateCanalAdeus(args, message, gc);
                break;
            case "mensagemadeus":
            case "mensagema":
            case "msgadeus":
                if (msg.length() > 2000) {
                    channel.sendMessage(":x: | Mensagem muito longo (Max. 2000)").queue();
                    return;
                }
                Settings.updateMsgAdeus(args, message, gc);
                break;
            case "tpoll":
            case "tempopoll":
                Settings.updatePollTime(args, message, gc);
                break;
            case "csug":
            case "canalsug":
                Settings.updateCanalSUG(args, message, gc);
                break;
            case "rwarn":
            case "rolewarn":
                Settings.updateCargoWarn(args, message, gc);
                break;
            case "ln":
            case "levelnotif":
                Settings.updateLevelNotif(args, message, gc);
                break;
            case "canallevelup":
            case "canallvlup":
            case "clvlup":
                Settings.updateCanalLevelUp(args, message, gc);
                break;
            case "canalrelay":
            case "canalrly":
            case "crelay":
                Settings.updateCanalRelay(args, message, gc);
                break;
            case "clvl":
            case "cargolevel":
            case "cargolvl":
                Settings.updateCargoLvl(args, message, gc);
                break;
            default:
                try {
                    Settings.embedConfig(message);
                } catch (IOException err) {
                    channel.sendMessage(":x: | Ocorreu um erro durante o processo, os meus developers já foram notificados.").queue();
                    Helper.log(this.getClass(), LogLevel.ERROR, err + " | " + err.getStackTrace()[0]);
                }
        }
    }
}
