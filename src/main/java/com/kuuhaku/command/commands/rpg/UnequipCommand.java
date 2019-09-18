package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Entities.Equipped;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class UnequipCommand extends Command {

	public UnequipCommand() {
		super("rdesequipar", new String[]{"runequip"}, "Desequipa um item", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
			Equipped inv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getInventory();
			inv.unequip(inv.getItem(String.join(" ", args)));
			channel.sendMessage("Item " + String.join(" ", args) + " desequipado.").queue();
		}
	}
}
