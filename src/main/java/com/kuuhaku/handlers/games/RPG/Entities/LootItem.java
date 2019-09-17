package com.kuuhaku.handlers.games.RPG.Entities;

import com.kuuhaku.handlers.games.RPG.Enums.Rarity;

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
