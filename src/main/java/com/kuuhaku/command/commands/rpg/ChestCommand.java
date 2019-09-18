package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Entities.Equipped;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.Arrays;

public class ChestCommand extends Command {

	public ChestCommand() {
		super("rloot", new String[]{"rchest"}, "Roda os espólios de um baú e dá ao jogador mencionado", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			if (message.getMentionedUsers().size() < 1) {
				channel.sendMessage(":x: | Você precisa especificar o jogador").queue();
				return;
			} else if (args.length < 2) {
				channel.sendMessage(":x: | O segundo argumento precisa ser o nome do baú").queue();
				return;
			}

			try {
				Equipped inv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getInventory();
				inv.addItem(Main.getInfo().getGames().get(guild.getId()).getChest(String.join(" ", Arrays.copyOfRange(args, 1, args.length))).dropLoot(Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getStatus().getLuck()));
			} catch (RuntimeException e) {
				channel.sendMessage(":x: | Baú não encontrado").queue();
			}
		}
	}
}
