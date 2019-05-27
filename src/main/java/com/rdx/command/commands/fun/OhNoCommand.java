package com.rdx.command.commands.fun;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import com.rdx.command.Category;
import com.rdx.command.Command;
import com.rdx.utils.Helper;

import java.io.File;
import java.io.IOException;

public class OhNoCommand extends Command {
	
	public OhNoCommand() {
		super("ohno", "<msg>", "Gera um meme no formato \"ohno\"", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		
		if(args.length<1) {
			channel.sendMessage(":x: | Voc� tem que escrever a mensagem que deseja que apareca na imagem.").queue();
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i<args.length; i++) {
			sb.append(args[i]).append(" ");
		}
		String str = sb.toString().trim();
		File f;
		try {
			f = Helper.createOhNoImage(str);
			channel.sendFile(f).complete();
		} catch (IOException e) {
			channel.sendMessage(":x: | Ocorreu um erro ao gerar a mensagem!").queue();
			return;
		}
		
		try {
			f.delete();
		} catch(Exception e) {}
	}

}
