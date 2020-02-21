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
import com.kuuhaku.controller.mysql.AccountDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.util.concurrent.TimeUnit;

public class PPTCommand extends Command {

	public PPTCommand() {
		super("jankenpon", new String[]{"ppt", "rps", "jokenpo", "janken"}, "<pedra/papel/tesoura>", "A Shiro joga jankenpo com você.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {

		if (args.length < 1) {
			channel.sendMessage(":x: | Você tem que escolher pedra, papel ou tesoura!").queue();
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
				channel.sendMessage(":x: | Você tem que escolher pedra, papel ou tesoura!").queue();
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
		channel.sendMessage("Sai").queue(p1 ->
				p1.editMessage(p1.getContentRaw() + "sho").queueAfter(333, TimeUnit.MILLISECONDS, p2 ->
						p2.editMessage(p2.getContentRaw() + " wa ").queueAfter(333, TimeUnit.MILLISECONDS, p3 ->
								p3.editMessage(p3.getContentRaw() + " guu!").queueAfter(333, TimeUnit.MILLISECONDS, p4 ->
										p4.editMessage(p4.getContentRaw() + "\nJan...").queueAfter(1, TimeUnit.SECONDS, p5 ->
												p5.editMessage(p5.getContentRaw() + "Ken...").queueAfter(1, TimeUnit.SECONDS, p6 ->
														p6.editMessage(p6.getContentRaw() + "Pon! " + finalPcChoice).queueAfter(1, TimeUnit.SECONDS, p7 -> {
															switch (finalWin) {
																case 0:
																	p7.editMessage(p7.getContentRaw() + "\nVocê perdeu!").queue();
																	break;
																case 1:
																	int crd = Helper.rng(10);
																	acc.addCredit(crd);
																	AccountDAO.saveAccount(acc);
																	p7.editMessage(p7.getContentRaw() + "\nVocê ganhou! Aqui, " + crd + " créditos por ter jogado comigo!").queue();
																	break;
																case 2:
																	p7.editMessage(p7.getContentRaw() + "\nEmpate!").queue();
																	break;
															}
														})
												)
										)
								)
						)
				)
		);
	}
}
