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
import com.kuuhaku.model.enums.AnimeName;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.persistent.Account;
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
import java.util.Objects;
import java.util.function.Function;

public class TwitchDrop implements Prize {
	private final AnimeName anime = AnimeName.validValues()[Helper.rng(AnimeName.validValues().length, true)];
	private final ExceedEnum exceed = ExceedEnum.values()[Helper.rng(ExceedEnum.values().length, true)];
	private final int[] values = {
			1 + Helper.rng((int) CardDAO.totalCards(anime) - 1, false),
			1 + Helper.rng(6, false),
			1 + Helper.rng((int) CardDAO.totalCards() - 1, false),
			Helper.rng(MemberDAO.getHighestLevel() / 2, false)
	};
	private final int amount = 250 + Helper.rng(1000, false);
	private final List<Pair<String, Function<EventUser, Boolean>>> requirement = new ArrayList<>() {{
		add(Pair.of("Ter " + values[2] + " carta" + (values[2] != 1 ? "s" : "") + " ou mais.", u ->
						KawaiponDAO
								.getKawaipon(Objects.requireNonNull(AccountDAO.getAccountByTwitchId(u.getId())).getUserId())
								.getCards()
								.size() >= values[2]
				)
		);

		add(Pair.of("Ter " + values[0] + " carta" + (values[0] != 1 ? "s" : "") + " de " + anime.toString() + " ou mais.", u ->
						KawaiponDAO
								.getKawaipon(Objects.requireNonNull(AccountDAO.getAccountByTwitchId(u.getId())).getUserId())
								.getCards()
								.stream()
								.filter(k -> k.getCard().getAnime().equals(anime))
								.count() >= values[0]
				)
		);

		add(Pair.of("Ser level " + values[3] + " ou maior.", u ->
						MemberDAO
								.getMemberByMid(Objects.requireNonNull(AccountDAO.getAccountByTwitchId(u.getId())).getUserId())
								.stream()
								.anyMatch(m -> m.getLevel() >= values[3])
				)
		);

		add(Pair.of("Ter até 1.000 créditos.", u ->
				Objects.requireNonNull(AccountDAO.getAccountByTwitchId(u.getId())).getBalance() <= 1000));

		add(Pair.of("Ter votado " + values[1] + " vez" + (values[1] != 1 ? "es" : "") + " seguida" + (values[1] != 1 ? "s" : "") + " ou mais.", u ->
				Objects.requireNonNull(AccountDAO.getAccountByTwitchId(u.getId())).getStreak() >= values[1]));

		add(Pair.of("Ser membro da " + exceed.getName() + ".", u ->
						ExceedDAO.hasExceed(Objects.requireNonNull(AccountDAO.getAccountByTwitchId(u.getId())).getUserId()) &&
						ExceedDAO.getExceedMember(u.getId()).getExceed().equalsIgnoreCase(exceed.getName())
				)
		);
	}};
	private final Pair<String, Function<EventUser, Boolean>> chosen = requirement.get(Helper.rng(requirement.size(), true));

	@Override
	public String getCaptcha() {
		return Helper.noCopyPaste(getRealCaptcha());
	}

	@Override
	public String getRealCaptcha() {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(ByteBuffer.allocate(4).putInt(hashCode()).array())).substring(0, 5);
		} catch (NoSuchAlgorithmException e) {
			return String.valueOf(System.currentTimeMillis()).substring(0, 5);
		}
	}

	@Override
	public void award(User u) {

	}

	@Override
	public void award(EventUser u) {
		Account acc = AccountDAO.getAccountByTwitchId(u.getId());
		assert acc != null;
		acc.addCredit(amount, this.getClass());
		AccountDAO.saveAccount(acc);
	}

	@Override
	public int getPrize() {
		return amount;
	}

	@Override
	public Consumable getPrizeAsItem() {
		return null;
	}

	@Override
	public String[] getPrizeWithPenalty() {
		return new String[0];
	}

	@Override
	public Pair<String, Function<User, Boolean>> getRequirement() {
		return null;
	}

	@Override
	public Map.Entry<String, Function<EventUser, Boolean>> getRequirementForTwitch() {
		return chosen;
	}
}
