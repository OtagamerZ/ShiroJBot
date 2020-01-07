package com.kuuhaku.handlers.games.rpg.entities;

import com.kuuhaku.handlers.games.rpg.enums.Rarity;

public class LootItem {
	private final Item item;
	private final Rarity rarity;

	public LootItem(Item item, Rarity rarity) {
		this.item = item;
		this.rarity = rarity;
	}

	public Item getItem() {
		return item;
	}

	public Rarity getRarity() {
		return rarity;
	}
}
