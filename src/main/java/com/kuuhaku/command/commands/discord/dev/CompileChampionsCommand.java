/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.dev;

import bsh.EvalError;
import bsh.Interpreter;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.EffectParameters;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

@Command(
		name = "testchamp",
		aliases = "tchamp",
		category = Category.DEV
)
public class CompileChampionsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		List<Champion> champions = CardDAO.getAllChampions(true);

		int errored = 0;
		for (Champion c : champions) {
			if (!c.hasEffect()) continue;
			String imports = EffectParameters.IMPORTS.formatted(c.getName());

			try {
				Interpreter i = new Interpreter();
				i.setStrictJava(true);
				i.set("ep", new EffectParameters());
				i.eval(imports + c.getRawEffect());
			} catch (EvalError e) {
				Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
				errored += 1;
			}
		}

		channel.sendMessage("""
				✅ Sucesso -> %s
				❌ Com erro -> %s
				""".formatted(champions.size() - errored, errored)).queue();
	}
}
