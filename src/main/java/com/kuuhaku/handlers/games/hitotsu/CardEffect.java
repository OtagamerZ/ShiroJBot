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

package com.kuuhaku.handlers.games.hitotsu;

import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.KawaiponRarity;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public enum CardEffect {
	BUY_2(KawaiponRarity.COMMON, (game, hand) -> {
		for (int i = 0; i < 2; i++) hand.draw(game.getDeque());
	}),
	SHUFFLE(KawaiponRarity.UNCOMMON, (game, hand) -> {
		game.shuffle();
	}),
	BLOCK(KawaiponRarity.RARE, (game, hand) -> {
		game.getTable().sendMessage(game.getPlayers().getUserSequence().getFirst().getAsMention() + " pulou a vez de " + hand.getUser().getAsMention()).queue();
		game.next();
	}),
	BUY_4(KawaiponRarity.ULTRA_RARE, (game, hand) -> {
		for (int i = 0; i < 4; i++) hand.draw(game.getDeque());
	}),
	SWAP_HANDS(KawaiponRarity.LEGENDARY, (game, hand) -> {
		Hand p1 = game.getHands().get(game.getPlayers().getUserSequence().getFirst());
		Hand p2 = game.getHands().get(game.getPlayers().getUserSequence().getLast());

		List<KawaiponCard> aux = p1.getCards();
		p1.setCards(p2.getCards());
		p2.setCards(aux);
	});

	private final KawaiponRarity rarity;
	private final BiConsumer<Hitotsu, Hand> effect;

	CardEffect(KawaiponRarity rarity, BiConsumer<Hitotsu, Hand> effect) {
		this.rarity = rarity;
		this.effect = effect;
	}

	public static BiConsumer<Hitotsu, Hand> getEffect(KawaiponRarity rarity) {
		return Arrays.stream(values()).filter(ce -> ce.rarity.equals(rarity)).findFirst().orElseThrow().effect;
	}
}
