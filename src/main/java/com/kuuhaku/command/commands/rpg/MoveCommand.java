package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Utils;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

public class MoveCommand extends Command {

	public MoveCommand() {
		super("rmover", new String[]{"rmove"}, "Move seu personagem 1 coordenada em qualquer direção", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		try {
			if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
				if (args.length == 0) {
					channel.sendMessage(":x: | Você precisa especificar as coordenadas.").queue();
					return;
				} else if (args[0].length() != 2) {
					channel.sendMessage(":x: | As coordenadas precisam ser concatenadas onde cada uma possui apenas 1 letra.").queue();
					return;
				}

				char[] coords = {args[0].charAt(0), args[0].charAt(1)};
				Integer[] numCoords = Utils.coordToArray(coords[0], coords[1]);

				if (numCoords[0] + numCoords[1] > 2) {
					channel.sendMessage(":x: | Você só pode andar uma casa por vez.").queue();
					return;
				}

				Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).move(Main.getInfo().getGames().get(guild.getId()).getCurrentMap(), Utils.coordToArray(coords[0], coords[1]));
				Main.getInfo().getGames().get(guild.getId()).render(message.getTextChannel()).queue();
			}
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
		}
	}
}
