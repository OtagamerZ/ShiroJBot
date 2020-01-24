/*
 * This file is part of Shiro J Bot.
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

public class PPTCommand extends Command {

	public PPTCommand() {
		super("ppt", new String[] {"rps"}, "<pedra/papel/tesoura>", "A Shiro joga pedra/papel/tesoura com você.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {

		if(args.length < 1) {
			channel.sendMessage(":x: | Você tem que escolher pedra, papel ou tesoura!").queue();
			return;
		}

		int pcOption = Helper.rng(3);
		int win = 2;

		switch(args[0].toLowerCase()) {
			case "pedra":
				switch(pcOption) {
					case 1:
						win = 0;
						break;
					case 2:
						win = 1;
						break;
				}
				break;
			case "papel":
				switch(pcOption) {
					case 0:
						win = 1;
						break;
					case 2:
						win = 0;
						break;
				}
				break;
			case "tesoura":
				switch (pcOption) {
					case 0:
						win = 0;
						break;
					case 1:
						win = 1;
						break;
				}
				break;
			default:
				channel.sendMessage(":x: | Você tem que escolher pedra, papel ou tesoura!").queue();
				return;
		}

		String pcChoice = "";

		switch(pcOption) {
			case 0:
				pcChoice = ":punch: **Pedra**";
				break;
			case 1:
				pcChoice = ":raised_back_of_hand: **Papel**";
				break;
			case 2:
				pcChoice = ":v: **Tesoura**";
				break;
		}

		switch(win) {
			case 0:
				channel.sendMessage("Perdeu! Eu escolhi " + pcChoice).queue();
				break;
			case 1:
				channel.sendMessage("Ganhou! Eu escolhi " + pcChoice).queue();
				break;
			case 2:
				channel.sendMessage("Empate! Eu escolhi " + pcChoice).queue();
				break;
		}
	}
}
