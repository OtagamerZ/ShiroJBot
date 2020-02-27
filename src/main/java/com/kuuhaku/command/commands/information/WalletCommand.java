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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.AccountDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

public class WalletCommand extends Command {

	public WalletCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public WalletCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public WalletCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public WalletCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Saldo de " + author.getName());
		eb.addField(":moneybag: | " + acc.getBalance() + " créditos", "Ultimo voto em: " + (acc.getLastVoted() == null ? "Nunca" : acc.getLastVoted()), true);
		eb.setColor(Helper.getRandomColor());
		eb.setThumbnail("https://lh3.googleusercontent.com/proxy/HPc-AN89uCegIfGS69Ii7Q-g2NhPzRxX1sJMrX_A80c8S7luf9LgFVt4vIsGiwgYRb77UVvyVAmhDlW4HQ7e5YUc-QWMqr8vAImtD1yADcP21MIHHfvDFAKJMD-Tf0ifrMCF23Y");

		channel.sendMessage(eb.build()).queue();
	}
}
