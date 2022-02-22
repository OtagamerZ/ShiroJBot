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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.states;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Phase;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.utils.Helper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GameState {
	private final Shoukan game;
	private final Map<Side, HandState> hands;
	private final Map<Side, List<SlotColumn>> slots;
	private final Map<Side, List<Drawable>> graveyard;
	private final List<Drawable> banned;
	private final List<Drawable> discardBatch;
	private final Set<PersistentEffect> persistentEffects;
	private final Field field;
	private final int fusionLock;
	private final int spellLock;
	private final int effectLock;
	private final boolean reroll;

	public GameState(Shoukan game) {
		this.game = game;
		this.hands = game.getHands().entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> new HandState(e.getValue())
				));
		this.slots = game.getArena().getSlots().entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> e.getValue().stream().map(SlotColumn::clone).toList()
				));
		this.graveyard = game.getArena().getGraveyard().entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> e.getValue().stream().map(Drawable::copy).toList()
				));
		this.banned = game.getArena().getBanned().stream()
				.map(Drawable::copy)
				.toList();
		this.discardBatch = game.getDiscardBatch().stream()
				.map(Drawable::copy)
				.toList();
		this.persistentEffects = game.getPersistentEffects().stream()
				.map(PersistentEffect::clone)
				.collect(Collectors.toSet());
		this.field = game.getArena().getField();
		this.fusionLock = game.getFusionLock();
		this.spellLock = game.getSpellLock();
		this.effectLock = game.getEffectLock();
		this.reroll = game.isReroll();
	}

	public void revertState() {
		for (Hand h : game.getHands().values()) {
			HandState old = hands.get(h.getSide());
			Helper.replaceContent(
					old.getDeque().stream().map(Drawable::copy).toList(),
					h.getRealDeque()
			);
			Helper.replaceContent(
					old.getCards().stream().map(Drawable::copy).toList(),
					h.getCards()
			);
			Helper.replaceContent(
					old.getDestinyDeck().stream().map(Drawable::copy).toList(),
					h.getDestinyDeck()
			);

			if (h.getHero() != null) {
				h.getHero().setHitpoints(old.getHero().getHitpoints());
				h.getHero().setXp(old.getHero().getXp());
			}

			h.setBaseHp(old.getBaseHp());
			h.setBaseManaPerTurn(old.getBaseManaPerTurn());
			h.setMitigation(old.getMitigation());
			h.setMaxCards(old.getMaxCards());
			h.setManaPerTurn(old.getManaPerTurn());
			h.setMana(old.getMana());
			h.setHp(old.getHp());
			h.setPrevHp(old.getPrevHp());
			h.setSuppressTime(old.getSuppressTime());
			h.setLockTime(old.getLockTime());
			h.setNullTime(old.getNullTime());

			Helper.replaceContent(
					graveyard.get(h.getSide()).stream().map(Drawable::copy).toList(),
					game.getArena().getGraveyard().get(h.getSide())
			);
		}

		for (Map.Entry<Side, List<SlotColumn>> e : game.getArena().getSlots().entrySet()) {
			List<SlotColumn> slots = this.slots.get(e.getKey());
			List<SlotColumn> value = e.getValue();
			for (int i = 0; i < value.size(); i++) {
				SlotColumn sc = value.get(i);
				SlotColumn old = slots.get(i);

				sc.setTop(old.getTop());
				sc.setBottom(old.getBottom());
				sc.setChanged(old.isChanged());
				sc.setUnavailable(old.getUnavailableTime());
			}
		}

		Helper.replaceContent(
				banned.stream().map(Drawable::copy).toList(),
				game.getArena().getBanned()
		);
		Helper.replaceContent(
				discardBatch.stream().map(Drawable::copy).toList(),
				game.getDiscardBatch()
		);
		Helper.replaceContent(
				persistentEffects.stream().map(PersistentEffect::clone).collect(Collectors.toSet()),
				game.getPersistentEffects()
		);
		game.getArena().setField(field);
		game.setFusionLock(fusionLock);
		game.setSpellLock(spellLock);
		game.setEffectLock(effectLock);
		game.setReroll(reroll);
		game.setPhase(Phase.PLAN);
		game.setState(this);
	}

	public Map<Side, HandState> getHands() {
		return hands;
	}

	public Map<Side, List<SlotColumn>> getSlots() {
		return slots;
	}

	public List<Drawable> getBanned() {
		return banned;
	}

	public List<Drawable> getDiscardBatch() {
		return discardBatch;
	}

	public Set<PersistentEffect> getPersistentEffects() {
		return persistentEffects;
	}

	public Field getField() {
		return field;
	}

	public int getFusionLock() {
		return fusionLock;
	}

	public int getSpellLock() {
		return spellLock;
	}

	public int getEffectLock() {
		return effectLock;
	}

	public boolean isReroll() {
		return reroll;
	}
}
