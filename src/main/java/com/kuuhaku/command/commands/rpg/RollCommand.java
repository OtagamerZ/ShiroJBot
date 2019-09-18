package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Utils;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.io.IOException;

public class RollCommand extends Command {

	public RollCommand() {
		super("rrolar", new String[]{"rdado"}, "Rola um ou mais dados seguindo o padrão D&D", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		try {
			channel.sendMessage(Utils.rollDice(String.join(" ", args), Main.getInfo().getGames().get(guild.getId()).getMaster() == author ? null : Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getStatus())).queue();
		} catch (Exception e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
		}
	}
}
