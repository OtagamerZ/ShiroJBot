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

package com.kuuhaku.model.common.drop;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.AnimeName;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singletonMap;

public class CreditDrop implements Prize {
	private final int[] values = {
			Helper.rng(5),
			Helper.rng(7),
			Helper.rng(10),
			Helper.rng(20),
			Helper.rng(AnimeName.values().length)
	};
	private final int amount = Helper.clamp(Helper.rng(1000), 250, 1000);
	private final List<Map<String, Function<User, Boolean>>> requirement = new ArrayList<>() {{
		add(singletonMap("Ter " + values[2] + " Kawaipons ou mais.", u ->
				Helper.getOr(KawaiponDAO.getKawaipon(u.getId()), new Kawaipon()).getCards().size() >= values[2]));

		add(singletonMap("Ter " + values[0] + " Kawaipons de " + AnimeName.values()[values[4]].toString() + ".", u -> {
			AnimeName an = AnimeName.values()[values[4]];
			return Helper.getOr(KawaiponDAO.getKawaipon(u.getId()), new Kawaipon()).getCards().stream().filter(k -> k.getAnime().equals(an)).count() >= values[0];
		}));

		add(singletonMap("Ser level " + values[3] + " ou maior.", u ->
				MemberDAO.getMemberByMid(u.getId()).stream().anyMatch(m -> m.getLevel() >= values[3])));

		add(singletonMap("Ter menos que 500 créditos.", u ->
				AccountDAO.getAccount(u.getId()).getBalance() < 500));

		add(singletonMap("Ter votado " + values[1] + " vezes seguidas.", u ->
				AccountDAO.getAccount(u.getId()).getStreak() >= values[1]));
	}};

	@Override
	public void award(User u) {
		Account acc = AccountDAO.getAccount(u.getId());
		acc.addCredit(amount);
		AccountDAO.saveAccount(acc);
	}

	@Override
	public int prize() {
		return amount;
	}

	@Override
	public Map.Entry<String, Function<User, Boolean>> getRequirement() {
		return requirement.get(Helper.rng(requirement.size())).entrySet().iterator().next();
	}
}
