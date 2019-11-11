package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Handlers.MobRegisterHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class NewMobCommand extends Command {

	public NewMobCommand() {
		super("rnovomonstro", new String[]{"rnewmob"}, "Inicia o cadastro de um novo monstro", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) new MobRegisterHandler(message.getTextChannel(), Main.getTet(), author, Main.getInfo().getGames().get(guild.getId()));
	}
}
