package com.kuuhaku.command.commands.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Anime;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.IOException;

public class PingCommand extends Command {
	
	public PingCommand() { super("ping", "Ping", Category.MISC); }

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		channel.sendMessage("Pong! :ping_pong: ").queue(msg -> msg.editMessage( msg.getContentRaw() + Main.getInfo().getPing() + " ms!").queue());
	}

}
