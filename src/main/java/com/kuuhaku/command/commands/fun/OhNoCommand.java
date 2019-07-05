package com.kuuhaku.command.commands.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.File;
import java.io.IOException;

public class OhNoCommand extends Command {
	
	public OhNoCommand() {
		super("ohno", "<mensagem>", "Gera um meme no formato \"ohno\"", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		
		if(args.length < 1) { channel.sendMessage(":x: | Você tem que escrever a mensagem que deseja que apareca no meme.").queue(); return; }
		
		String sb = String.join(" ", args);
		String str = sb.trim();
		File f;
		try {
			f = Helper.createOhNoImage(str);
			channel.sendFile(f).complete();
		} catch (IOException e) {
			channel.sendMessage(":x: | Ocorreu um erro ao gerar a mensagem!").queue();
		}
	}

}
