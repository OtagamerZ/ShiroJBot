package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Actors.Actor;
import com.kuuhaku.handlers.games.RPG.Handlers.CombatHandler;
import com.kuuhaku.handlers.games.RPG.Handlers.PvPHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class SwitchMapCommand extends Command {

	public SwitchMapCommand() {
		super("raomapa", new String[]{"rtrocarmapa", "rtomap"}, "Muda de mapa", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			if (args.length == 0) {
				channel.sendMessage(":x: | É necessário especificar o número do mapa").queue();
				return;
			}
			try {
				Main.getInfo().getGames().get(guild.getId()).switchMap(Integer.parseInt(args[0]));
			} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
				channel.sendMessage(":x: | Índice inválido, existem " + Main.getInfo().getGames().get(guild.getId()).getMaps().size() + " mapas cadastrados.").queue();
			}
		}
	}
}
