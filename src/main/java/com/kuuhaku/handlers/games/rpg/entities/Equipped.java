package com.kuuhaku.handlers.games.rpg.entities;

import com.kuuhaku.handlers.games.rpg.exceptions.NoSlotAvailableException;
import com.kuuhaku.handlers.games.rpg.exceptions.UnknownItemException;

import java.util.ArrayList;
import java.util.List;

public class Equipped extends Inventory {
	private Item.Head head = null;
	private Item.Chest chest = null;
	private Item.Leg leg = null;
	private Item.Foot boot = null;
	private Item.Arm glove = null;
	private final List<Item.Ring> rings = new ArrayList<>();
	private Item.Neck necklace = null;
	private Item.Weapon weapon = null;
	private Item.Weapon offhand = null;
	private Item.Bag bag = null;

	public int[] getStatModifiers() {
		int defense =
				(head == null ? 0 : head.getDefense()) +
						(chest == null ? 0 : chest.getDefense()) +
						(leg == null ? 0 : leg.getDefense()) +
						(boot == null ? 0 : boot.getDefense()) +
						(glove == null ? 0 : glove.getDefense()) +
						(weapon == null ? 0 : weapon.getDefense()) +
						(offhand == null ? 0 : offhand.getDefense());
		int strength =
				(glove == null ? 0 : glove.getStrength()) +
						(weapon == null ? 0 : weapon.getStrength()) +
						(offhand == null ? 0 : offhand.getStrength());
		int perception =
				(necklace == null ? 0 : necklace.getPerception());
		int endurance =
				(chest == null ? 0 : chest.getEndurance());
		int charisma = rings.stream().mapToInt(Item.Ring::getCharisma).sum();
		int intelligence = rings.stream().mapToInt(Item.Ring::getIntelligence).sum() +
						(weapon == null ? 0 : weapon.getIntelligence());
		int agility =
				(boot == null ? 0 : boot.getAgility());
		int luck =
				(leg == null ? 0 : leg.getLuck()) + rings.stream().mapToInt(Item.Ring::getLuck).sum() +
						(necklace == null ? 0 : necklace.getLuck());

		return new int[]{defense, strength, perception, endurance, charisma, intelligence, agility, luck};
	}

	public void equip(Item item) throws NoSlotAvailableException, UnknownItemException {
		if (!getItems().contains(item)) throw new UnknownItemException();
		switch (item.getType()) {
			case HEAD:
				if (head != null) throw new NoSlotAvailableException();
				head = (Item.Head) item;
				break;
			case CHEST:
				if (chest != null) throw new NoSlotAvailableException();
				chest = (Item.Chest) item;
				break;
			case LEG:
				if (leg != null) throw new NoSlotAvailableException();
				leg = (Item.Leg) item;
				break;
			case FOOT:
				if (boot != null) throw new NoSlotAvailableException();
				boot = (Item.Foot) item;
				break;
			case ARM:
				if (glove != null) throw new NoSlotAvailableException();
				glove = (Item.Arm) item;
				break;
			case NECK:
				if (necklace != null) throw new NoSlotAvailableException();
				necklace = (Item.Neck) item;
				break;
			case BAG:
				if (bag != null) throw new NoSlotAvailableException();
				bag = (Item.Bag) item;
				setMaxSize(bag.getCapacity());
				break;
			case RING:
				if (rings.size() == 6) throw new NoSlotAvailableException();
				rings.add((Item.Ring) item);
				break;
			case WEAPON:
				if (weapon != null && offhand != null) throw new NoSlotAvailableException();
				else if (weapon != null) offhand = (Item.Weapon) item;
				else weapon = (Item.Weapon) item;
				break;
		}
		removeItem(item);
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	public void unequip(Item item) throws UnknownItemException, NoSlotAvailableException {
		if (getItems().size() == getMaxSize()) throw new NoSlotAvailableException();
		switch (item.getType()) {
			case HEAD:
				if (head == null) throw new UnknownItemException();
				head = null;
				break;
			case CHEST:
				if (chest == null) throw new UnknownItemException();
				chest = null;
				break;
			case LEG:
				if (leg == null) throw new UnknownItemException();
				leg = null;
				break;
			case FOOT:
				if (boot == null) throw new UnknownItemException();
				boot = null;
				break;
			case ARM:
				if (glove == null) throw new UnknownItemException();
				glove = null;
				break;
			case NECK:
				if (necklace == null) throw new UnknownItemException();
				necklace = null;
				break;
			case BAG:
				if (bag == null) throw new UnknownItemException();
				bag = null;
				break;
			case RING:
				if (!rings.contains(item)) throw new UnknownItemException();
				rings.remove(item);
				break;
			case WEAPON:
				if (weapon == null) throw new UnknownItemException();
				weapon = null;
				break;
			default:
				throw new UnknownItemException();
		}
		addItem(item);
	}

	public Item.Head getHead() {
		return head;
	}

	public Item.Chest getChest() {
		return chest;
	}

	public Item.Leg getLeg() {
		return leg;
	}

	public Item.Foot getBoot() {
		return boot;
	}

	public Item.Arm getGlove() {
		return glove;
	}

	public List<Item.Ring> getRings() {
		return rings;
	}

	public Item.Neck getNecklace() {
		return necklace;
	}

	public Item.Weapon getWeapon() {
		return weapon;
	}

	public Item.Weapon getOffhand() {
		return offhand;
	}

	public Item.Bag getBag() {
		return bag;
	}
}
