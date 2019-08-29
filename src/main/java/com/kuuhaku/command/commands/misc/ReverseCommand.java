package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class ReverseCommand extends Command {
	
	public ReverseCommand() {
		super("reverse", new String[] {"inverter"}, "<texto>", "Inverte o texto fornecido.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		
		if(args.length < 1) { channel.sendMessage(":x: | Você precisa de indicar o texto que deseja inverter.").queue(); return; }
		
        String txt = String.join(" ", args);
        
        txt = new StringBuilder(txt.trim()).reverse().toString();
        channel.sendMessage(txt).queue();
	}

}
