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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.DailyQuest;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Map;


public class DailyQuestCommand extends Command {

	public DailyQuestCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public DailyQuestCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public DailyQuestCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public DailyQuestCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		DailyQuest dq = DailyQuest.getQuest(author.getId());
		Account acc = AccountDAO.getAccount(author.getId());
		Map<DailyTask, Integer> pg = acc.getDailyProgress();

		if (dq.checkTasks(pg)) {
			channel.sendMessage("Você já completou os desafios diários de hoje, volte amanhã!").queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Desafios diários de " + author.getName())
				.setThumbnail("https://static.wikia.nocookie.net/dauntless_gamepedia_en/images/6/60/Quest_Main_Available_Icon_001.png")
				.setDescription("Completar estes desafios lhe dará créditos (3500 multiplicado pela dificuldade) e 1 gema caso a dificuldade esteja acima de 3.5x.")
				.addField("Obtida?", Helper.isTrustedMerchant(author.getId()) ? "SIM!!" : "Não", true)
				.addField("Modificador de dificuldade", Helper.roundToString(dq.getDifficultyMod(), 1) + "x", true);

		for (Map.Entry<DailyTask, Integer> task : dq.getTasks().entrySet()) {
			eb.addField(
					task.getKey().getName(),
					(pg.get(task.getKey()) >= task.getValue() ? "`COMPLETADO`" : "%s | Atual: %s").formatted(
							task.getKey().getDescription().formatted(Helper.separate(task.getValue())),
							Helper.separate(pg.getOrDefault(task.getKey(), 0))
					),
					false
			);
		}

		channel.sendMessage(eb.build()).queue();
	}
}
