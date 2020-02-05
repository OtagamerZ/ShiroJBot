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
import net.dv8tion.jda.api.entities.*;

import java.util.Random;

public class ChooseCommand extends Command {

	public ChooseCommand() {
		super("escolha", new String[]{"choose"}, "<opção 1;opção 2;...>", "Pede para a Shiro escolher uma das opções informadas. (Opções separadas por ponto-e-vírgula)", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | Você não me deu nenhuma opção.").queue();
			return;
		} else if (!args[0].contains(";")) {
			channel.sendMessage(":x: | Você precisa me dar ao menos duas opções.").queue();
			return;
		}

		String[] opts = args[0].split(";");
		long seed = 0;

		for (char c : args[0].toCharArray()) {
			seed += (int) c;
		}

		int choice = new Random(seed).nextInt(opts.length);

		channel.sendMessage(":question: | Eu escolho a opção " + choice + ": " + opts[choice] + "!").queue();
	}
}
