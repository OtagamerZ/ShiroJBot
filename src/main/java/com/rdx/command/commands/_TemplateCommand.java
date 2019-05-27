package com.rdx.command.commands;

import com.rdx.command.Command;
import com.rdx.command.Category;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class _TemplateCommand extends Command {
	
	public _TemplateCommand() {
		super("NomeDoComando", new String[] {"Aliase1", "Aliase2"}, "<arg_obrigat�rio> [arg_opcional]", "Descri��o do comando.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		
	}

}
