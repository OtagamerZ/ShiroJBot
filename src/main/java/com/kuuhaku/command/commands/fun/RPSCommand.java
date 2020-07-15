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

package com.kuuhaku.command.commands.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class RPSCommand extends Command {

	public RPSCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public RPSCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public RPSCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public RPSCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {

		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_rock-paper-scissors-invalid-arguments")).queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());

		int pcOption = Helper.rng(3);
		int win = 2;

		switch (args[0].toLowerCase()) {
			case "pedra":
			case ":punch:":
				switch (pcOption) {
					case 1:
						win = 0;
						break;
					case 2:
						win = 1;
						break;
				}
				break;
			case "papel":
			case ":raised_back_of_hand:":
				switch (pcOption) {
					case 0:
						win = 1;
						break;
					case 2:
						win = 0;
						break;
				}
				break;
			case "tesoura":
			case ":v:":
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
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_rock-paper-scissors-invalid-arguments")).queue();
				return;
		}

		String pcChoice = "";

		switch (pcOption) {
			case 0:
				pcChoice = ":punch:";
				break;
			case 1:
				pcChoice = ":raised_back_of_hand:";
				break;
			case 2:
				pcChoice = ":v:";
				break;
		}

		int finalWin = win;
		String finalPcChoice = pcChoice;
		channel.sendMessage("Saisho wa guu!\nJan...Ken...Pon! " + finalPcChoice)
				.queue(m -> {
					switch (finalWin) {
						case 0:
							m.editMessage(m.getContentRaw() + "\nVocê perdeu!").queue();

							if (ExceedDAO.hasExceed(author.getId())) {
								PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
								ps.modifyInfluence(false);
								PStateDAO.savePoliticalState(ps);
							}
							break;
						case 1:
							int crd = Helper.rng(50);
							acc.addCredit(crd);
							AccountDAO.saveAccount(acc);
							m.editMessage(m.getContentRaw() + "\nVocê ganhou! Aqui, " + crd + " créditos por ter jogado comigo!").queue();

							if (ExceedDAO.hasExceed(author.getId())) {
								PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
								ps.modifyInfluence(2);
								PStateDAO.savePoliticalState(ps);
							}
							break;
						case 2:
							m.editMessage(m.getContentRaw() + "\nEmpate!").queue();
							break;
					}
				});
	}
}
