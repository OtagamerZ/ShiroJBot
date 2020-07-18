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
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.AnimeName;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CreditDrop implements Prize {
	private final AnimeName anime = AnimeName.values()[Helper.rng(AnimeName.values().length, true)];
	private final ExceedEnums exceed = ExceedEnums.values()[Helper.rng(ExceedEnums.values().length, true)];
	private final int[] values = {
			Helper.rng((int) CardDAO.totalCards(anime), false),
			1 + Helper.rng(6, false),
			Helper.rng((int) CardDAO.totalCards(), false),
			Helper.rng(MemberDAO.getHighestLevel() / 2, false)
	};
	private final int amount = Helper.clamp(Helper.rng(1250, false), 250, 1250);
	private final List<Pair<String, Function<User, Boolean>>> requirement = new ArrayList<>() {{
		add(Pair.of("Ter " + values[2] + " Kawaipons ou mais.", u ->
				KawaiponDAO.getKawaipon(u.getId()).getCards().size() >= values[2]));

		add(Pair.of("Ter " + values[0] + " Kawaipons de " + anime.toString() + ".", u ->
				KawaiponDAO.getKawaipon(u.getId()).getCards().stream().filter(k -> k.getCard().getAnime().equals(anime)).count() >= values[0]));

		add(Pair.of("Ser level " + values[3] + " ou maior.", u ->
				MemberDAO.getMemberByMid(u.getId()).stream().anyMatch(m -> m.getLevel() >= values[3])));

		add(Pair.of("Ter até 1000 créditos.", u ->
				AccountDAO.getAccount(u.getId()).getBalance() <= 1000));

		add(Pair.of("Ter votado " + values[1] + " vezes seguidas ou mais.", u ->
				AccountDAO.getAccount(u.getId()).getStreak() >= values[1]));

		add(Pair.of("Ser membro da " + exceed.getName() + ".", u ->
				ExceedDAO.hasExceed(u.getId()) && ExceedDAO.getExceedMember(u.getId()).getExceed().equalsIgnoreCase(exceed.getName())));
	}};
	private final Pair<String, Function<User, Boolean>> chosen = requirement.get(Helper.rng(requirement.size(), true));

	@Override
	public String getCaptcha() {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(ByteBuffer.allocate(4).putInt(hashCode()).array())).substring(0, 5);
		} catch (NoSuchAlgorithmException e) {
			return String.valueOf(System.currentTimeMillis());
		}
	}

	@Override
	public void award(User u) {
		Account acc = AccountDAO.getAccount(u.getId());
		acc.addCredit(amount);
		AccountDAO.saveAccount(acc);
	}

	@Override
	public int getPrize() {
		return amount;
	}

	@Override
	public Pair<String, Function<User, Boolean>> getRequirement() {
		return chosen;
	}
}
