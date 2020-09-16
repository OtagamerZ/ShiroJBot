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

package com.kuuhaku.command.commands.twitch;

import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.enums.CommandPermission;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.common.events.domain.EventUser;
import com.kuuhaku.Main;
import com.kuuhaku.command.TwitchCommand;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;

import java.util.Set;

public class CatchKawaiponCommand extends TwitchCommand {

	public CatchKawaiponCommand(String name, String description, boolean requiresBinding) {
		super(name, description, requiresBinding);
	}

	public CatchKawaiponCommand(String name, String[] aliases, String description, boolean requiresBinding) {
		super(name, aliases, description, requiresBinding);
	}

	public CatchKawaiponCommand(String name, String usage, String description, boolean requiresBinding) {
		super(name, usage, description, requiresBinding);
	}

	public CatchKawaiponCommand(String name, String[] aliases, String usage, String description, boolean requiresBinding) {
		super(name, aliases, usage, description, requiresBinding);
	}

	@Override
	public void execute(EventUser author, Account account, String rawCmd, String[] args, ChannelMessageEvent message, EventChannel channel, TwitchChat chat, Set<CommandPermission> permissions) {
		Account acc = AccountDAO.getAccountByTwitchId(author.getId());
		assert acc != null;
		Kawaipon kp = KawaiponDAO.getKawaipon(acc.getUserId());

		KawaiponCard kc = Main.getInfo().getCurrentCard().getIfPresent("twitch");

		if (kc == null) {
			chat.sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_no-card"));
			return;
		}

		int cost = kc.getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * (kc.isFoil() ? 2 : 1);
		if (acc.getBalance() < cost) {
			chat.sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user"));
			return;
		}

		if (kp.getCards().contains(kc)) {
			chat.sendMessage(channel.getName(), ShiroInfo.getLocale(I18n.PT).getString("err_card-owned"));
			return;
		}

		Main.getInfo().getCurrentCard().invalidate("twitch");
		kp.addCard(kc);
		acc.removeCredit(cost, this.getClass());

		KawaiponDAO.saveKawaipon(kp);
		AccountDAO.saveAccount(acc);

		chat.sendMessage(channel.getName(), author.getName() + " adquiriu a carta `" + kc.getName() + "` com sucesso!");
	}
}
