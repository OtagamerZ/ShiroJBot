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

package com.kuuhaku.command.commands.discord.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.StaffDAO;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.StaffType;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

import javax.persistence.NoResultException;

@Command(
		name = "bug",
		aliases = {"hunter", "debug"},
		usage = "req_mention-id",
		category = Category.DEV
)
public class BugHuntCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() > 0) {
			resolveBugHuntByMention(message, channel);
		} else {
			try {
				if (Main.getInfo().getUserByID(args[0]) != null) {
					try {
						resolveBugHuntById(args, channel);
					} catch (NoResultException e) {
						TagDAO.addUserTagsToDB(args[0]);
						resolveBugHuntById(args, channel);
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				channel.sendMessage(I18n.getString("err_no-user")).queue();
			}
		}
	}

	private void resolveBugHuntById(String[] args, MessageChannel channel) {
		Account acc = AccountDAO.getAccount(args[0]);

		acc.addBug();

		boolean staff = StaffDAO.getUser(args[0]).getType().isAllowed(StaffType.TESTER);
		int cr = 1000 * (staff ? 2 : 1);

		if (acc.getBugs() % 5 == 0) {
			cr *= 2.5;
			int slots = 10 * (staff ? 2 : 1);

			acc.addCredit(cr, this.getClass());
			acc.setCardStashCapacity(acc.getCardStashCapacity() + slots);
			channel.sendMessage("<@" + args[0] + "> ajudou a matar um bug! (+" + Helper.separate(cr) + " CR e +" + slots + " espaços no armazém)").queue();
		} else {
			acc.addCredit(cr, this.getClass());
			channel.sendMessage("<@" + args[0] + "> ajudou a matar um bug! (+" + Helper.separate(cr) + " CR)").queue();
		}

		AccountDAO.saveAccount(acc);
	}

	private void resolveBugHuntByMention(Message message, MessageChannel channel) {
		Account acc = AccountDAO.getAccount(message.getMentionedUsers().get(0).getId());

		acc.addBug();

		boolean staff = StaffDAO.getUser(acc.getUid()).getType().isAllowed(StaffType.TESTER);
		int cr = 1000 * (staff ? 2 : 1);

		if (acc.getBugs() % 5 == 0) {
			cr *= 2.5;
			int slots = 10 * (staff ? 2 : 1);

			acc.addCredit(cr, this.getClass());
			acc.setCardStashCapacity(acc.getCardStashCapacity() + slots);
			channel.sendMessage("<@" + acc.getUid() + "> ajudou a matar um bug! (+" + Helper.separate(cr) + " CR e +" + slots + " espaços no armazém)").queue();
		} else {
			acc.addCredit(cr, this.getClass());
			channel.sendMessage("<@" + acc.getUid() + "> ajudou a matar um bug! (+" + Helper.separate(cr) + " CR)").queue();
		}

		AccountDAO.saveAccount(acc);
	}
}
