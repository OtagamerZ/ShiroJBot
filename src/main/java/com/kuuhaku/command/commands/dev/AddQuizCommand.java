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
import com.kuuhaku.controller.postgresql.QuizDAO;
import com.kuuhaku.model.persistent.Quiz;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;

public class AddQuizCommand extends Command {

	public AddQuizCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public AddQuizCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public AddQuizCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public AddQuizCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Você precisa informar os argumentos necessários para a construção do quiz (pergunta;[alternativas];alt. correta;prêmio).").queue();
			return;
		}

		String[] quizArgs = String.join(" ", args).split(";");
		JSONArray opts = new JSONArray(quizArgs[1]);

		if (quizArgs.length < 4) {
			channel.sendMessage(":x: | Você precisa informar os argumentos necessários para a construção do quiz (pergunta;[alternativas];alt. correta;prêmio).").queue();
			return;
		} else if (opts.length() != 4) {
			channel.sendMessage(":x: | Cada quiz deve conter 4 alternativas.").queue();
			return;
		} else if (!StringUtils.isNumeric(quizArgs[2])) {
			channel.sendMessage(":x: | A alternativa correta deve ser um valor inteiro entre 1 e 4.").queue();
			return;
		}

		int o = Integer.parseInt(quizArgs[2]);

		if (o < 1 || o > 4) {
			channel.sendMessage(":x: | A alternativa correta deve ser um valor numérico entre 1 e 4.").queue();
			return;
		}

		Quiz q = new Quiz(quizArgs[0], opts, o, Integer.parseInt(quizArgs[3]));
		QuizDAO.saveQuiz(q);

		channel.sendMessage("Quiz adicionado com sucesso!").queue();
	}
}
