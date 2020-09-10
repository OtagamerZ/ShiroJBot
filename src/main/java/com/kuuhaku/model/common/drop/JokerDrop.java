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

import com.github.twitch4j.common.events.domain.EventUser;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.Consumable;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.AnimeName;
import com.kuuhaku.utils.ConsumableShop;
import com.kuuhaku.utils.ExceedEnum;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JokerDrop implements Prize {
	private final AnimeName anime = AnimeName.values()[Helper.rng(AnimeName.values().length, true)];
	private final ExceedEnum exceed = ExceedEnum.values()[Helper.rng(ExceedEnum.values().length, true)];
	private final int[] values = {
			1 + Helper.rng((int) CardDAO.totalCards(anime) - 1, false),
			1 + Helper.rng(6, false),
			1 + Helper.rng((int) CardDAO.totalCards() - 1, false),
			Helper.rng(MemberDAO.getHighestLevel() / 2, false)
	};
	private final int amount = Helper.clamp(Helper.rng(5000, false), 2500, 5000);
	private final Consumable prize = new ArrayList<>(ConsumableShop.getAvailable().values()).get(Helper.rng(ConsumableShop.getAvailable().size(), true));
	private final int penalty = Helper.clamp(Helper.rng(5000, false), 3750, 7500);
	private final List<Pair<String, Function<User, Boolean>>> requirement = new ArrayList<>() {{
		add(Pair.of("Ter " + values[2] + " carta" + (values[2] != 1 ? "s" : "") + " ou mais.", u ->
				KawaiponDAO.getKawaipon(u.getId()).getCards().size() >= values[2]));

		add(Pair.of("Ter " + values[0] + " carta" + (values[0] != 1 ? "s" : "") + " de " + anime.toString() + " ou mais.", u ->
				KawaiponDAO.getKawaipon(u.getId()).getCards().stream().filter(k -> k.getCard().getAnime().equals(anime)).count() >= values[0]));

		add(Pair.of("Ser level " + values[3] + " ou maior.", u ->
				MemberDAO.getMemberByMid(u.getId()).stream().anyMatch(m -> m.getLevel() >= values[3])));

		add(Pair.of("Ter até 1000 créditos.", u ->
				AccountDAO.getAccount(u.getId()).getBalance() <= 1000));

		add(Pair.of("Ter votado " + values[1] + " vez" + (values[1] != 1 ? "es" : "") + " seguidas ou mais.", u ->
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
		acc.addCredit(amount, this.getClass());
		acc.addLoan(penalty);
		AccountDAO.saveAccount(acc);
	}

	@Override
	public void award(EventUser u) {

	}

	@Override
	public int getPrize() {
		return 0;
	}

	@Override
	public Consumable getPrizeAsItem() {
		return null;
	}

	@Override
	public Object[] getPrizeWithPenalty() {
		return new Object[]{Helper.rng(100, false) > 50 ? prize : amount, penalty};
	}

	@Override
	public Pair<String, Function<User, Boolean>> getRequirement() {
		return chosen;
	}

	@Override
	public Map.Entry<String, Function<EventUser, Boolean>> getRequirementForTwitch() {
		return null;
	}
}
