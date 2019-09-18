package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.io.IOException;

public class MapCommand extends Command {

	public MapCommand() {
		super("rmapa", new String[]{"rmap"}, "Vê o mapa atual", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		try {
			Main.getInfo().getGames().get(guild.getId()).render(message.getTextChannel()).queue();
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
		} catch (NullPointerException e) {
			channel.sendMessage(":x: | Não há nenhum mapa ativo.").queue();
		}
	}
}
