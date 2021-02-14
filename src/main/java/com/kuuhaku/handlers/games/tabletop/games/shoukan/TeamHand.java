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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.handlers.games.tabletop.utils.InfiniteList;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import java.util.*;

public class TeamHand extends Hand {
	private final InfiniteList<User> users = new InfiniteList<>();
	private final InfiniteList<LinkedList<Drawable>> deques = new InfiniteList<>();
	private final InfiniteList<List<Drawable>> cards = new InfiniteList<>();
	private final InfiniteList<List<Drawable>> destinyDecks = new InfiniteList<>();

	public TeamHand(Shoukan game, List<User> users, List<Kawaipon> kps, Side side) {
		super(game, null, kps.get(0), null);
		for (int i = 0; i < users.size(); i++) {
			Kawaipon kp = kps.get(i);
			User user = users.get(i);

			LinkedList<Drawable> deque = new LinkedList<>() {{
				addAll(kp.getChampions());
			}};
			List<Drawable> destinyDeck = new ArrayList<>();

			deque.sort(Comparator
					.comparing(d -> ((Champion) d).getMana()).reversed()
					.thenComparing(c -> ((Champion) c).getCard().getName(), String.CASE_INSENSITIVE_ORDER)
			);
			deque.addAll(kp.getEquipments());
			deque.addAll(kp.getFields());

			Account acc = AccountDAO.getAccount(user.getId());
			for (Drawable d : deque) d.setAcc(acc);

			this.users.add(user);

			if (game.getCustom() != null) {
				if (game.getCustom().optBoolean("semequip"))
					getDeque().removeIf(d -> d instanceof Equipment);
				if (game.getCustom().optBoolean("semfield"))
					getDeque().removeIf(d -> d instanceof Field);

				switch (game.getCustom().optString("arcade")) {
					case "roleta" -> {
						for (Drawable d : deque) {
							if (d instanceof Champion) {
								Champion c = (Champion) d;
								c.setRawEffect("""
										if (ep.getTrigger() == EffectTrigger.ON_ATTACK) {
											int rng = Math.round(Math.random() * 100);
											if (rng < 25) {
												Hand h = ep.getHands().get(ep.getSide());
												h.setHp(h.getHp() / 2);
											} else if (rng < 50) {
												Hand h = ep.getHands().get(ep.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);
												h.setHp(h.getHp() / 2);
											}
										}
										%s
										""".formatted(Helper.getOr(c.getRawEffect(), "")));
							}
						}
					}
					case "blackrock" -> {
						Field f = CardDAO.getField("OTHERWORLD");
						assert f != null;
						f.setAcc(AccountDAO.getAccount(user.getId()));
						game.getArena().setField(f);
						deque.removeIf(d -> d instanceof Champion || d instanceof Field);
						for (String name : new String[]{"MATO_KUROI", "SAYA_IRINO", "YOMI_TAKANASHI", "YUU_KOUTARI", "TAKU_KATSUCHI", "KAGARI_IZURIHA"}) {
							Champion c = CardDAO.getChampion(name);
							deque.addAll(Collections.nCopies(6, c));
						}
						for (Drawable d : deque) d.setAcc(acc);
					}
					case "instakill" -> deque.removeIf(d -> d instanceof Equipment && ((Equipment) d).getCharm() != null && ((Equipment) d).getCharm() == Charm.SPELL);
				}
			}

			if (kp.getDestinyDraw() != null) {
				int champs = kp.getChampions().size();
				for (int x : kp.getDestinyDraw()) {
					if (x > champs) {
						destinyDeck.clear();
						break;
					} else
						destinyDeck.add(deque.get(x));
				}
			}
			for (Drawable drawable : destinyDeck) {
				deque.remove(drawable);
			}

			this.deques.add(deque);
			this.destinyDecks.add(destinyDeck);
			this.cards.add(new ArrayList<>());
		}

		for (int i = 0; i < this.users.size(); i++, next()) {
			redrawHand();
		}
	}

	public void next() {
		users.getNext();
		deques.getNext();
		cards.getNext();
		destinyDecks.getNext();
	}

	public String getMentions() {
		return getUser().getAsMention() + " e " + users.peekNext().getAsMention();
	}

	public List<String> getNames() {
		return List.of(getUser().getName(), users.peekNext().getName());
	}
}
