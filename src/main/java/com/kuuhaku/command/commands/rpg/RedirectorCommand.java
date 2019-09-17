package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Commands.MasterCommands;
import com.kuuhaku.handlers.games.RPG.Commands.PlayerCommands;
import com.kuuhaku.handlers.games.RPG.World.World;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.Arrays;

public class RedirectorCommand extends Command {

	public RedirectorCommand() {
		super("rpg", "Permite o uso dos comandos do módulo RPG", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()) == null) {
			if (args.length > 0 && args[0].equalsIgnoreCase("novo") && Helper.hasPermission(member, PrivilegeLevel.MOD)) {
				channel.sendMessage("Campanha iniciada com sucesso.\nMestre: " + author.getAsMention()).queue();
				Main.getInfo().getGames().put(guild.getId(), new World(author));
				return;
			} else {
				channel.sendMessage(":x: | Este servidor ainda não possui uma campanha de RPG.").queue();
				return;
			}
		}

		World world = Main.getInfo().getGames().get(guild.getId());

		if (args.length < 2) {
			channel.sendMessage(":x: | São necessários ao menos 2 argumentos.").queue();
			return;
		}
		if (world.getMaster() == author) {
			new MasterCommands().execute(world.getCurrentMap(), Main.getTet(), author, world, message.getTextChannel(), message, args[1], Arrays.copyOfRange(args, 1, args.length));
		} else {
			new PlayerCommands().execute(world.getCurrentMap(), Main.getTet(), author, world, message.getTextChannel(), message, args[1], Arrays.copyOfRange(args, 1, args.length));
		}
	}
}
