package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Handlers.ItemRegisterHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class NewItemCommand extends Command {

	public NewItemCommand() {
		super("rnovoitem", new String[]{"rnewitem"}, "Inicia o cadastro de um novo item", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) new ItemRegisterHandler(message.getTextChannel(), Main.getTet(), author);
	}
}
