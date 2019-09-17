package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Actors.Actor;
import com.kuuhaku.handlers.games.RPG.Entities.Equipped;
import com.kuuhaku.handlers.games.RPG.Handlers.CombatHandler;
import com.kuuhaku.handlers.games.RPG.Handlers.PvPHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.Arrays;

public class GiveCommand extends Command {

	public GiveCommand() {
		super("rdar", new String[]{"rgive"}, "Dá um item ou dinheiro à outro jogador", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(":x: | Você precisa especificar o jogador").queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage(":x: | O segundo argumento precisa ser o nome do item ou ouro").queue();
			return;
		} else if (args.length < 3 && (args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) {
			channel.sendMessage(":x: | Você precisa especificar a quantidade de ouro").queue();
			return;
		}

		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			Equipped inv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getInventory();
			if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro")))
				inv.addGold(Integer.parseInt(args[2]));
			else inv.addItem(Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
			return;
		}

		Equipped selfInv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getInventory();
		Equipped targetInv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getInventory();
		if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) {
			selfInv.addGold(-Integer.parseInt(args[2]));
			targetInv.addGold(Integer.parseInt(args[2]));
		} else {
			selfInv.removeItem(selfInv.getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
			targetInv.addItem(Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
		}
	}
}
