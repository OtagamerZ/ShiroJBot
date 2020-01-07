package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.rpg.entities.Equipped;
import net.dv8tion.jda.api.entities.*;

public class UnequipCommand extends Command {

	public UnequipCommand() {
		super("rdesequipar", new String[]{"runequip"}, "<item>", "Desequipa um item.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
			Equipped inv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getInventory();
			inv.unequip(inv.getItem(String.join(" ", args)));
			channel.sendMessage("Item " + String.join(" ", args) + " desequipado.").queue();
		}
	}
}
