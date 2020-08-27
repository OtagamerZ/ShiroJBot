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

package com.kuuhaku.handlers.games.tabletop.games.hitotsu;

import com.kuuhaku.utils.KawaiponRarity;

import java.util.Arrays;
import java.util.function.BiConsumer;

public enum CardEffect {
	BUY_2(KawaiponRarity.COMMON, (game, hand) -> {
		game.getChannel().sendMessage(game.getCurrent().getAsMention() + " jogou um `+2` para " + hand.getUser().getAsMention() + "!").queue();
		for (int i = 0; i < 2; i++) hand.draw(game.getDeque());
	}),
	SHUFFLE(KawaiponRarity.UNCOMMON, (game, hand) -> {
		game.getChannel().sendMessage(game.getCurrent().getAsMention() + " embaralhou o deque!").queue();
		game.shuffle();
	}),
	BLOCK(KawaiponRarity.RARE, (game, hand) -> {
		game.getChannel().sendMessage(game.getCurrent().getAsMention() + " pulou a vez de " + hand.getUser().getAsMention() + "!").queue();
		game.resetTimer();
		game.getSeats().get(game.getCurrent().getId()).showHand();
	}),
	BUY_4(KawaiponRarity.ULTRA_RARE, (game, hand) -> {
		game.getChannel().sendMessage(game.getCurrent().getAsMention() + " jogou um `+4` para " + hand.getUser().getAsMention() + "!").queue();
		for (int i = 0; i < 4; i++) hand.draw(game.getDeque());
	}),
	SWAP_HANDS(KawaiponRarity.LEGENDARY, (game, hand) -> {
		game.getChannel().sendMessage(game.getCurrent().getAsMention() + " jogou um `+2` para todo mundo!").queue();
		for (Hand h : game.getSeats().values())
			for (int i = 0; i < 4; i++)
				h.draw(game.getDeque());
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
