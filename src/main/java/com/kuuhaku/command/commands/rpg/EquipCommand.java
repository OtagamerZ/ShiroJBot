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

public class EquipCommand extends Command {

	public EquipCommand() {
		super("requipar", new String[]{"requip"}, "Equipa um item do inventário", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
			Equipped inv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getInventory();
			inv.equip(inv.getItem(String.join(" ", args)));
			channel.sendMessage("Item " + String.join(" ", args) + " equipado.").queue();
		}
	}
}
