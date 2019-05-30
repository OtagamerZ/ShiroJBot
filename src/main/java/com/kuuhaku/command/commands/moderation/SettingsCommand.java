package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class SettingsCommand extends Command {

	public SettingsCommand() {
		super("settings", new String[] {"defenições", "defeniçoes", "defenicões", "parametros", "parâmetros"}, "<parâmetro> <novo valor do parâmetro>", "Muda as configurações da Shiro no seu servidor.", Category.MODERACAO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		switch(args[0].toLowerCase()) {
			case "prefix":
				String newPrefix = args[1].trim();
				System.out.println(newPrefix);
				if(newPrefix.length() > 5) { channel.sendMessage(":x: | O prefixo `" + newPrefix + "` contem mais de 5 carateres, não pode.").queue(); return; }

				guildConfig gc = SQLite.getGuildById(guild.getId());
				SQLite.updateGuildPrefix(newPrefix, gc);
				break;
			default:
				//channel.sendMessage("").queue();
				return;
		}

	}

}
