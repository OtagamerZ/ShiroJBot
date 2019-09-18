package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Handlers.MobRegisterHandler;
import com.kuuhaku.handlers.games.RPG.Handlers.PlayerRegisterHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class NewPlayerCommand extends Command {

	public NewPlayerCommand() {
		super("rnovo", new String[]{"rnew"}, "Inicia seu cadastro como jogador", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getCurrentMap() == null) {
			channel.sendMessage(":x: | Ainda não existe nenhum mapa marcado como ativo, espere o mestre da campanha criá-lo").queue();
			return;
		}
		new PlayerRegisterHandler(Main.getInfo().getGames().get(guild.getId()).getCurrentMap(), message.getTextChannel(), Main.getTet(), author);
	}
}
