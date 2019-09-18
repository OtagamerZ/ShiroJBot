package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.awt.*;
import java.io.IOException;

public class EquippedCommand extends Command {

	public EquippedCommand() {
		super("requipados", new String[]{"requipped"}, "Mostra os itens equipados", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() != author) {
			try {
				Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().openNiceInventory(message.getTextChannel()).queue();
			} catch (IOException | FontFormatException e) {
				Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
			}
		}
	}
}
