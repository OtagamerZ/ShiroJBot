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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.RatingDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.SupportRating;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

import java.time.LocalDateTime;

@Command(
		name = "obrigado",
		aliases = {"obrigada", "valeu", "thanks", "thx", "vlw"},
		usage = "req_mention",
		category = Category.MISC
)
public class ThanksCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage("Você ainda possui " + acc.getThanksTokens() + " de 4 tokens de suporte.").queue();
			return;
		} else if (ShiroInfo.getStaff().contains(author.getId())) {
			channel.sendMessage("❌ | Membros da equipe da Shiro não podem dar tokens de suporte.").queue();
			return;
		} else if (!ShiroInfo.getStaff().contains(message.getMentionedUsers().get(0).getId())) {
			channel.sendMessage("❌ | Você só pode dar tokens de suporte à membros da equipe da Shiro.").queue();
			return;
		}

		LocalDateTime time = LocalDateTime.now();
		if (acc.getThanksTokens() <= 0) {
			int rem = time.getHour() % 6;
			channel.sendMessage("❌ | Você não possui tokens de suporte restantes, você irá receber 1 em " + (rem == 1 ? rem + " hora" : rem + "horas") + ".").queue();
			return;
		}

		acc.useThanksToken();
		AccountDAO.saveAccount(acc);

		SupportRating sr = RatingDAO.getRating(message.getMentionedUsers().get(0).getId());
		sr.addThanksToken(author.getId());
		RatingDAO.saveRating(sr);

		channel.sendMessage("✅ | Suporte agradecido com sucesso.").queue();
	}
}