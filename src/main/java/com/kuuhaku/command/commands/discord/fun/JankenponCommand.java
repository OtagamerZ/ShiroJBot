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

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Leaderboards;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.entities.*;

import java.util.Locale;

@Command(
		name = "pedrapapeltesoura",
		aliases = {"rockpaperscissors", "jankenpon", "rps", "ppt", "jkp"},
		usage = "req_rockpaperscissors",
		category = Category.FUN
)
public class JankenponCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(I18n.getString("err_rock-paper-scissors-invalid-arguments")).queue();
			return;
		}

		Account acc = Account.find(Account.class, author.getId());
		int pcOption = MathHelper.rng(2);
		int win = 2;

		switch (args[0].toLowerCase(Locale.ROOT)) {
			case "pedra", ":punch:" -> win = switch (pcOption) {
				case 1 -> 0;
				case 2 -> 1;
				default -> win;
			};
			case "papel", ":raised_back_of_hand:" -> win = switch (pcOption) {
				case 0 -> 1;
				case 2 -> 0;
				default -> win;
			};
			case "tesoura", ":v:" -> win = switch (pcOption) {
				case 0 -> 0;
				case 1 -> 1;
				default -> win;
			};
			default -> {
				channel.sendMessage(I18n.getString("err_rock-paper-scissors-invalid-arguments")).queue();
				return;
			}
		}

		String pcChoice = switch (pcOption) {
			case 0 -> ":punch:";
			case 1 -> ":raised_back_of_hand:";
			case 2 -> ":v:";
			default -> "";
		};

		int finalWin = win;
		channel.sendMessage("Saisho wa guu!\nJan...Ken...Pon! " + pcChoice + (
				switch (finalWin) {
					case 0 -> {
						int lost = Leaderboards.queryNative(Number.class, "SELECT COALESCE(SUM(l.score), 0) FROM Leaderboards l WHERE l.uid = :uid AND l.minigame = :game LIMIT 10",
								author.getId(),
								getThis().getSimpleName()
						).intValue();
						if (lost > 0) {
							new Leaderboards(author, getThis(), -lost).save();
						}
						yield "\nVocê perdeu!";
					}
					case 1 -> {
						int crd = MathHelper.rng(35, 125);
						acc.addCredit(crd, this.getClass());
						acc.save();
						new Leaderboards(author, getThis(), 1).save();
						yield "\nVocê ganhou! Aqui, " + StringHelper.separate(crd) + " CR por ter jogado comigo!";
					}
					case 2 -> "\nEmpate!";
					default -> throw new IllegalStateException("Unexpected value: " + finalWin);
				}
		)).queue();
	}
}
