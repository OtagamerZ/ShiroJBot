package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

public class RestartCommand extends Command {

	public RestartCommand() {
		super("restart", new String[]{"reset"}, "Reinicia a Shiro!", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if(author.getId().equals(Main.getInfo().getNiiChan())) {
			channel.sendMessage("Matane, Nii-chan! <3").queue();
		} else {
			channel.sendMessage("Iniciando o protocolo de reinicialização...").queue();
		}

		try {
			Main.getJibril().shutdown();
			Main.getInfo().getAPI().shutdown();
			Main.main(Main.getArgs());
		} catch (Exception e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
