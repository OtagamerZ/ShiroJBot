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

import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.common.interfaces.Prize;
import com.kuuhaku.model.enums.ClanTier;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public abstract class Drop<P> implements Prize<P> {
	private final AddedAnime anime;
	private final ClanTier tier;
	private final RankedTier ranked;
	private final int[] values;
	private final List<Pair<String, Function<User, Boolean>>> condition;
	private final Pair<String, Function<User, Boolean>> chosen;
	private final P prize;
	private String captcha;

	private final int total = Card.queryNative(Number.class, "SELECT COUNT(1) FROM Card").intValue();

	protected Drop(P prize) {
		List<AddedAnime> animes = AddedAnime.queryAll(AddedAnime.class, "SELECT a FROM AddedAnime a WHERE a.hidden = FALSE");;
		anime = CollectionHelper.getRandomEntry(animes);
		tier = CollectionHelper.getRandomEntry(ClanTier.values());
		ranked = CollectionHelper.getRandomEntry(ArrayUtils.subarray(RankedTier.values(), 1, RankedTier.values().length));
		values = new int[]{
				MathHelper.rng(1, Card.queryNative(Number.class, "SELECT COUNT(1) FROM Card c WHERE c.anime_name = :anime", anime.getName()).intValue()),
				MathHelper.rng(1, 7),
				MathHelper.rng(1, Card.queryNative(Number.class, "SELECT COUNT(1) FROM Card").intValue() / 2),
				MathHelper.rng(1, MemberDAO.getHighestLevel() / 2)
		};
		condition = new ArrayList<>() {{
			add(Pair.of("Ter " + values[2] + " carta" + (values[2] != 1 ? "s" : "") + " ou mais",
					u -> Kawaipon.find(Kawaipon.class, u.getId()).getCards().size() >= values[2]));

			add(Pair.of("Ter " + values[0] + " carta" + (values[0] != 1 ? "s" : "") + " de " + anime.toString() + " ou mais",
					u -> Kawaipon.find(Kawaipon.class, u.getId()).getCards().stream().filter(k -> k.getCard().getAnime().equals(anime)).count() >= values[0]));

			add(Pair.of("Ser level " + values[3] + " ou maior",
					u -> MemberDAO.getMembersByUid(u.getId()).stream().anyMatch(m -> m.getLevel() >= values[3])));

			add(Pair.of("Ter até 1.000 CR",
					u -> Account.find(Account.class, u.getId()).getBalance() <= 1000));

			add(Pair.of("Ter votado " + values[1] + " vez" + (values[1] != 1 ? "es" : "") + " seguida" + (values[1] != 1 ? "s" : "") + " ou mais",
					u -> Account.find(Account.class, u.getId()).getStreak() >= values[1]));

			add(Pair.of("Estar em um clã com tier " + tier.getName().toLowerCase(Locale.ROOT) + " ou superior",
					u -> {
						Clan c = ClanDAO.getUserClan(u.getId());
						if (c == null) return false;
						else return c.getTier().ordinal() >= tier.ordinal();
					}));

			add(Pair.of("Possuir ranking " + ranked.getName() + " no Shoukan ou superior",
					u -> MatchMakingRatingDAO.getMMR(u.getId()).getTier().ordinal() >= ranked.ordinal()));
		}};
		chosen = CollectionHelper.getRandomEntry(condition);
		this.prize = prize;
	}

	public AddedAnime getAnime() {
		return anime;
	}

	public int[] getValues() {
		return values;
	}

	public List<Pair<String, Function<User, Boolean>>> getCondition() {
		return condition;
	}

	public Pair<String, Function<User, Boolean>> getChosen() {
		return chosen;
	}

	@Override
	public String getCaptcha() {
		return StringHelper.noCopyPaste(getRealCaptcha());
	}

	@Override
	public String getRealCaptcha() {
		if (captcha == null)
			captcha = CollectionHelper.getOr(StringHelper.generateRandomHash(6), String.valueOf(System.currentTimeMillis()).substring(0, 6));
		return captcha;
	}

	@Override
	public P getPrize() {
		return prize;
	}

	@Override
	public Map.Entry<String, Function<User, Boolean>> getRequirement() {
		return chosen;
	}

	@Override
	public void awardInstead(User u, int prize) {
		Account acc = Account.find(Account.class, u.getId());
		acc.addCredit(prize, this.getClass());

		if (acc.hasPendingQuest()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.merge(DailyTask.DROP_TASK, 1, Integer::sum);
			acc.setDailyProgress(pg);
		}

		acc.save();
	}
}
