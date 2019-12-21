package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.awt.*;
import java.io.IOException;

public class EquippedCommand extends Command {

	public EquippedCommand() {
		super("requipados", new String[]{"requipped"}, "Mostra os itens equipados.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
			try {
				Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().openNiceInventory(message.getTextChannel()).queue();
			} catch (IOException | FontFormatException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		}
	}
}
