package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.rpg.Utils;
import com.kuuhaku.handlers.games.rpg.entities.Equipped;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;

public class GiveCommand extends Command {

	public GiveCommand() {
		super("rdar", new String[]{"rgive"}, "<@usuário> <item/ouro> [qtd de ouro]", "Dá um item ou dinheiro à outro jogador.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
        if (Utils.noPlayerAlert(args, message, channel)) return;

        if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			if (args.length < 3 && (args[1].equalsIgnoreCase("xp") || args[1].equalsIgnoreCase("ouro"))) {
				channel.sendMessage(":x: | Você precisa especificar a quantidade de XP").queue();
				return;
			} else if (args.length < 3 && (args[1].equalsIgnoreCase("dano"))) {
				channel.sendMessage(":x: | Você precisa especificar a quantidade de dano").queue();
				return;
			}

			Equipped inv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getInventory();
			if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro")))
				inv.addGold(Integer.parseInt(args[2]));
			else if ((args[1].equalsIgnoreCase("xp")))
				Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getStatus().addXp(Integer.parseInt(args[2]));
			else if ((args[1].equalsIgnoreCase("dano")))
				Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getStatus().trueDamage(Integer.parseInt(args[2]));
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
