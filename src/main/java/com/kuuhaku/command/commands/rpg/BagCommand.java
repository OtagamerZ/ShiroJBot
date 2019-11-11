package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class BagCommand extends Command {

	public BagCommand() {
		super("rbolsa", new String[]{"rbag"}, "Abre a mochila de seu personagem", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
			Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().openInventory(message.getTextChannel()).queue();
		}
	}
}
