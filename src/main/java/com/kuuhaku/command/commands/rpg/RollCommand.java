package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.rpg.Utils;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

public class RollCommand extends Command {

	public RollCommand() {
		super("rrolar", new String[]{"rdado"}, "<função de dados>", "Rola um ou mais dados seguindo o padrão D&D.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
			try {
				channel.sendMessage(Utils.rollDice(String.join(" ", args), Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getStatus())).queue();
			} catch (Exception e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		} else if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			try {
				channel.sendMessage(Utils.rollDice(String.join(" ", args), null)).queue();
			} catch (Exception e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		}

	}
}
