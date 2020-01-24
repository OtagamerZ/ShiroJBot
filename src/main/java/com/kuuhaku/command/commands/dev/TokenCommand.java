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

package com.kuuhaku.command.commands.dev;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.ArrayUtils;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenCommand extends Command {

	public TokenCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public TokenCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public TokenCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public TokenCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Preciso do nome do usuário para adicionar na geração do token").queue();
			return;
		}

		SecureRandom sr = new SecureRandom();
		byte[] nameSpace = args[0].getBytes();
		byte[] randomSpace = new byte[48 - nameSpace.length];
		sr.nextBytes(randomSpace);

		channel.sendMessage("Token de acesso do usuário " + args[0] + ": `" + Base64.getEncoder().encodeToString(ArrayUtils.addAll(nameSpace, randomSpace)) + "`\n\n__**Este token é aleatório, ele precisa ser ativado antes de poder ser usado (caso contrário é apenas uma String sem valor algum)**__").queue();
	}
}
