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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.EffectParameters;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.List;

public class CompileChampionsCommand extends Command {

	public CompileChampionsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public CompileChampionsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public CompileChampionsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public CompileChampionsCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		List<Champion> champions = CardDAO.getAllChampions(true);

		for (Champion c : champions) {
			if (!c.hasEffect()) continue;

			String imports = """
					//%s
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Phase;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.ArenaField;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hand;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
					import com.kuuhaku.handlers.games.tabletop.games.shoukan.SlotColumn;
					import com.kuuhaku.controller.postgresql.AccountDAO;
					import com.kuuhaku.controller.postgresql.CardDAO;
					import com.kuuhaku.model.enums.AnimeName;
					import com.kuuhaku.utils.Helper;
					import org.json.JSONArray;
					          				
					          """.formatted(c.getName());

			try {
				Interpreter i = new Interpreter();
				i.setStrictJava(true);
				i.set("ep", new EffectParameters());
				i.eval(imports + c.getRawEffect());
			} catch (EvalError e) {
				Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
			}
		}
	}
}
