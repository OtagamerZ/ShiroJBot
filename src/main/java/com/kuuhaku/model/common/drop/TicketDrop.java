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

package com.kuuhaku.model.common.drop;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.DynamicParameterDAO;
import com.kuuhaku.model.persistent.DynamicParameter;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.math.NumberUtils;

public class TicketDrop extends Drop<String> {
	public TicketDrop() {
		super("");
	}

	@Override
	public void award(User u) {
		Role beta = Main.getInfo().getRoleByID("1003859593656533032");
		Guild g = beta.getGuild();
		Member m = g.getMemberById(u.getId());

		if (m != null) {
			g.addRoleToMember(m, beta).queue();
		} else {
			u.openPrivateChannel()
					.flatMap(s -> s.sendMessage("Você deve entrar no servidor de suporte para poder participar do beta fechado\nhttps://discord.com/invite/9sgkzna"))
					.queue();
		}

		Main.getInfo().getUserByID(ShiroInfo.getNiiChan())
				.openPrivateChannel()
				.flatMap(s -> s.sendMessage(u.getName() + " encontrou um ticket dourado"))
				.queue();

		DynamicParameter dp = DynamicParameterDAO.getParam("golden_tickets");
		int rem = NumberUtils.toInt(dp.getValue());
		DynamicParameterDAO.setParam("golden_tickets", String.valueOf(rem + 1));
	}

	@Override
	public String toString() {
		return "Ticket de acesso beta";
	}

	@Override
	public String toString(User u) {
		return toString();
	}
}
