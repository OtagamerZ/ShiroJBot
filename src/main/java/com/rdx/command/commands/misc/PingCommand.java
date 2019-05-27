package com.rdx.command.commands.misc;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import com.rdx.command.Category;
import com.rdx.command.Command;

public class PingCommand extends Command {
	
	public PingCommand() { super("ping", "Ping", Category.MISC); }

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		message.getChannel().sendMessage("Pong! :ping_pong:").queue((msg) -> msg.editMessage( "Ping: " + event.getJDA().getPing() + " ms!").queue());
	}

}
