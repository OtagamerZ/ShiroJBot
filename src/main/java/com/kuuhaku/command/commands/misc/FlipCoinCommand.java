package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class FlipCoinCommand extends Command {
	
	public FlipCoinCommand() {
		super("flipcoin", new String[] {"caracoroa", "headstails"}, "Lança uma moeda ao ar. (Cara-ou-Coroa)", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		
		channel.sendMessage("Calculando cara ou coroa....").queue((msg) -> msg.editMessage(((new Random()).nextBoolean() ? "<@" + author.getId() + "> **CARA** :smile:!" : "<@" + author.getId() + "> **COROA** :crown:!")).queueAfter(750, TimeUnit.MILLISECONDS));
		
	}

}
