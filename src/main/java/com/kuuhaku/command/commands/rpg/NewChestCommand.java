package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.rpg.handlers.ChestRegisterHandler;
import net.dv8tion.jda.api.entities.*;

public class NewChestCommand extends Command {

	public NewChestCommand() {
		super("rnovobau", new String[]{"rnew"}, "Inicia seu cadastro como jogador.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) new ChestRegisterHandler(message.getTextChannel(), Main.getTet(), author, Main.getInfo().getGames().get(guild.getId()));
	}
}
