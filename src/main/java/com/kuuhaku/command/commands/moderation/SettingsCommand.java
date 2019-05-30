package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.IOException;

public class SettingsCommand extends Command {

	public SettingsCommand() {
		super("settings", new String[] {"setting", "definições", "definiçoes", "definicões", "parametros", "parâmetros"}, "<parâmetro> <novo valor do parâmetro>", "Muda as configurações da Shiro no seu servidor.", Category.MODERACAO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		guildConfig gc = SQLite.getGuildById(guild.getId());

		if(args.length == 0) {
			try {
				Helper.embedConfig(message);
			} catch (IOException err) {
				channel.sendMessage(":x: | Ocorreu um erro durante o processo, os meus developers já foram notificados.").queue();
				err.printStackTrace();
				return;
			}

			return;
		}

		switch(args[0].toLowerCase()) {
			case "prefix":

				if(args.length < 2) { channel.sendMessage("O prefixo atual deste servidor é `" + prefix + "`.").queue(); return; }

				String newPrefix = args[1].trim();
				if(newPrefix.length() > 5) { channel.sendMessage(":x: | O prefixo `" + newPrefix + "` contem mais de 5 carateres, não pode.").queue(); return; }

				SQLite.updateGuildPrefix(newPrefix, gc);
				channel.sendMessage("✅ | O prefixo deste servidor foi trocado para `" + newPrefix + "` com sucesso.").queue();
				break;
			default:
				try {
					Helper.embedConfig(message);
				} catch (IOException err) {
					channel.sendMessage(":x: | Ocorreu um erro durante o processo, os meus developers já foram notificados.").queue();
					err.printStackTrace();
					return;
				}
		}
	}
}
