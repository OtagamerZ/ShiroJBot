package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class SayCommand extends Command {
	
	public SayCommand() {
		super("say", new String[] {"dizer", "repetir"}, "<mensagem>", "Repete a mensagem definida no canal atual.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		
		if(args.length == 0) { channel.sendMessage(":x: | Você precisa definir uma mensagem.").queue(); return; }
		
        String mensagem = "";

        for (int i = 0; i < args.length; i++) {
			mensagem += args[i] + " ";
        }

		mensagem = mensagem.trim();
        
		channel.sendMessage(mensagem).queue();
	}

}
