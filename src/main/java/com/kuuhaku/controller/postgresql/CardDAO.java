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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CardDAO extends DAO {
	private static final KawaiponRarity[] blacklist = {KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD};

	public static List<Card> getCards() {
		return queryAll(Card.class, "SELECT c FROM Card c WHERE c.anime.hidden = FALSE AND rarity NOT IN :blacklist",
				EnumSet.of(KawaiponRarity.ULTIMATE, blacklist)
		);
	}

	public static List<Card> getCards(List<String> ids) {
		if (ids.isEmpty()) return new ArrayList<>();

		return queryAll(Card.class, "SELECT c FROM Card c WHERE c.id IN :names AND c.anime.hidden = FALSE AND rarity NOT IN :blacklist",
				ids,
				EnumSet.of(KawaiponRarity.ULTIMATE, blacklist)
		);
	}

	public static List<Card> getCards(String anime) {
		return queryAll(Card.class, "SELECT c FROM Card c WHERE c.anime.name = :anime AND rarity NOT IN :blacklist",
				anime,
				EnumSet.of(KawaiponRarity.ULTIMATE, blacklist)
		);
	}

	public static List<Card> getCards(KawaiponRarity rarity) {
		return queryAll(Card.class, "SELECT c FROM Card c WHERE c.anime.hidden = FALSE AND rarity = :rarity", rarity);
	}

	public static Card getCard(String name) {
		return query(Card.class, "SELECT c FROM Card c WHERE id = UPPER(:name) AND rarity NOT IN :blacklist",
				name,
				EnumSet.of(KawaiponRarity.ULTIMATE, blacklist)
		);
	}

	public static Card getCard(String name, boolean withUltimate) {
		return query(Card.class, "SELECT c FROM Card c WHERE id = UPPER(:name) AND c.anime.hidden = FALSE AND rarity NOT IN :blacklist",
				name,
				withUltimate ? Set.of(blacklist) : EnumSet.of(KawaiponRarity.ULTIMATE, blacklist)
		);
	}

	public static Card getRandomCard() {
		return queryNative(Card.class, "SELECT c FROM Card c WHERE c.rarity NOT IN :blacklist ORDER BY RANDOM() LIMIT 1",
				EnumSet.of(KawaiponRarity.ULTIMATE, blacklist)
		);
	}

	public static CardType identifyType(String id) {
		return queryNative(CardType.class, "SELECT \"GetCardType\"(:id)", id);
	}
}
