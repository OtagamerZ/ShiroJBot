/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.misc;

import com.expression.parser.exception.CalculatorException;
import com.expression.parser.function.FunctionX;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.helpers.MathHelper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "calcular",
		aliases = {"math", "calc"},
		usage = "req_eqation",
		category = Category.MISC
)
public class MathCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar uma equação.").queue();
			return;
		}

		try {
			FunctionX func = new FunctionX(argsAsText);
			channel.sendMessage("f(x) = " + MathHelper.roundToString(func.getF_xo(0), 5)).queue();
		} catch (CalculatorException e) {
			channel.sendMessage("❌ | Equação inválida.").queue();
		}
	}
}
