package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Exceptions.UnknownItemException;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class ViewCommand extends Command {

	public ViewCommand() {
		super("rver", new String[]{"rinfo"}, "<item>", "Vê a descrição de um item", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		try {
			if (args.length == 0) {
				channel.sendMessage(":x: | É necessário especificar o nome do item").queue();
				return;
			}
			if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
				Main.getInfo().getGames().get(guild.getId()).getItem(args[0]).info(message.getTextChannel()).queue();
				return;
			}

			Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getInventory().getItem(args[0]).info(message.getTextChannel()).queue();
		} catch (UnknownItemException e) {
			channel.sendMessage(":x: | Nenhum item encontrado com esse nome").queue();
		}
	}
}
