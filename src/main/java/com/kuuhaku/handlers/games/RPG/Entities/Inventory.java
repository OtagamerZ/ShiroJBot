package com.kuuhaku.handlers.games.RPG.Entities;

import com.kuuhaku.handlers.games.RPG.Exceptions.UnknownItemException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Inventory {
	private final List<Item> items = new ArrayList<>();
	private final int defaultSize = 5;
	private int maxSize = defaultSize;
	private int gold = 0;

	public void addItem(Item item) {
		items.add(item);
	}

	public void removeItem(Item item) {
		items.remove(item);
	}

	public List<Item> getItems() {
		return items;
	}

	public Item getItem(String name) throws UnknownItemException {
		List<Item> its = items.stream().filter(i -> i.getName().equalsIgnoreCase(name)).collect(Collectors.toList());
		if (its.size() > 0) return its.get(0).getThis();
		else throw new UnknownItemException();
	}

	public int getDefaultSize() {
		return defaultSize;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = getDefaultSize() + maxSize;
	}

	public int getMaxNameLength(Graphics2D g2d) {
		assert items.size() > 0;
		return items.stream().mapToInt(i -> g2d.getFontMetrics().stringWidth(i.getName())).max().getAsInt();
	}

	public int getGold() {
		return gold;
	}

	public void addGold(int gold) {
		this.gold += gold;
	}
}
