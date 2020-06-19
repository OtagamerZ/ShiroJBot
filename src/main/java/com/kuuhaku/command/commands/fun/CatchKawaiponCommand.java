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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.KawaiponCard;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class CatchKawaiponCommand extends Command {

	public CatchKawaiponCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public CatchKawaiponCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public CatchKawaiponCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public CatchKawaiponCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		KawaiponCard kc = ShiroInfo.getCurrentCard().getIfPresent(guild.getId());

		if (kc == null) {
			channel.sendMessage(":x: | Não há nenhuma carta Kawaipon para coletar neste servidor.").queue();
			return;
		}

		int cost = (6 - kc.getRarity().getIndex()) * 250;
		if (acc.getBalance() < cost) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
			return;
		}

		if (kp == null) {
			kp = new Kawaipon();
			kp.setUid(author.getId());
		}

		if (kp.getCards().contains(kc)) {
			channel.sendMessage(":x: | Você já possui esta carta.").queue();
			return;
		}

		ShiroInfo.getCurrentCard().invalidate(guild.getId());
		kp.addCard(kc);
		acc.removeCredit(cost);

		KawaiponDAO.saveKawaipon(kp);
		AccountDAO.saveAccount(acc);

		channel.sendMessage("Você adquiriu a carta `" + kc.getName() + "` com sucesso!").queue();
	}
}
