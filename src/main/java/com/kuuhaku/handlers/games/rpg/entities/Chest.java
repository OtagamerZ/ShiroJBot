package com.kuuhaku.handlers.games.rpg.entities;

import com.kuuhaku.handlers.games.rpg.Utils;

import java.util.List;

public class Chest {
	private final String name;
	private final List<LootItem> lootTable;

	public Chest(String name, List<LootItem> loot) {
		this.name = name;
		this.lootTable = loot;
	}

	public String getName() {
		return name;
	}

	public Item dropLoot(int luck) {
		return Utils.getItem(luck, lootTable);
	}
}
