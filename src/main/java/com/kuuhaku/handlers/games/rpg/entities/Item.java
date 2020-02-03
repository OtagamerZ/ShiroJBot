/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.handlers.games.rpg.entities;

import com.kuuhaku.handlers.games.rpg.enums.Equipment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

@SuppressWarnings("ALL")
public abstract class Item {
	private final String name;
	private final String description;
	private final String image;
	private final Equipment type;
	private final int price;

	private Item(String name, String description, String image, Equipment type, int price) {
		this.name = name;
		this.description = description;
		this.image = image;
		this.type = type;
		this.price = price;
	}

	public static class Head extends Item {
		private final int defense;
		private final int intelligence;

		public Head(String name, String description, String image, int price, int defense, int intelligence) {
			super(name, description, image, Equipment.HEAD, price);
			this.defense = defense;
			this.intelligence = intelligence;
		}

		public int getDefense() {
			return defense;
		}

		public int getIntelligence() {
			return intelligence;
		}

		@Override
		public RestAction info(TextChannel channel) {
			EmbedBuilder eb = infoModel();

			eb.addField("Defesa", String.valueOf(defense), true);
			eb.addField("Inteligência", String.valueOf(intelligence), true);

			return channel.sendMessage(eb.build());
		}
	}

	public static class Chest extends Item {
		private final int defense;
		private final int endurance;

		public Chest(String name, String description, String image, int price, int defense, int endurance) {
			super(name, description, image, Equipment.CHEST, price);
			this.defense = defense;
			this.endurance = endurance;
		}

		public int getDefense() {
			return defense;
		}

		public int getEndurance() {
			return endurance;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public RestAction info(TextChannel channel) {
			EmbedBuilder eb = infoModel();

			eb.addField("Defesa", String.valueOf(defense), true);
			eb.addField("Resistência", String.valueOf(endurance), true);

			return channel.sendMessage(eb.build());
		}
	}

	public static class Leg extends Item {
		private final int defense;
		private final int luck;

		public Leg(String name, String description, String image, int price, int defense, int luck) {
			super(name, description, image, Equipment.LEG, price);
			this.defense = defense;
			this.luck = luck;
		}

		public int getDefense() {
			return defense;
		}

		public int getLuck() {
			return luck;
		}

		@Override
		public RestAction info(TextChannel channel) {
			EmbedBuilder eb = infoModel();

			eb.addField("Defesa", String.valueOf(defense), true);
			eb.addField("Sorte", String.valueOf(luck), true);

			return channel.sendMessage(eb.build());
		}
	}

	public static class Foot extends Item {
		private final int defense;
		private final int agility;

		public Foot(String name, String description, String image, int price, int defense, int agility) {
			super(name, description, image, Equipment.FOOT, price);
			this.defense = defense;
			this.agility = agility;
		}

		public int getDefense() {
			return defense;
		}

		public int getAgility() {
			return agility;
		}

		@Override
		public RestAction info(TextChannel channel) {
			EmbedBuilder eb = infoModel();

			eb.addField("Defesa", String.valueOf(defense), true);
			eb.addField("Agilidade", String.valueOf(agility), true);

			return channel.sendMessage(eb.build());
		}
	}

	public static class Arm extends Item {
		private final int defense;
		private final int strength;

		public Arm(String name, String description, String image, int price, int defense, int strength) {
			super(name, description, image, Equipment.ARM, price);
			this.defense = defense;
			this.strength = strength;
		}

		public int getDefense() {
			return defense;
		}

		public int getStrength() {
			return strength;
		}

		@Override
		public RestAction info(TextChannel channel) {
			EmbedBuilder eb = infoModel();

			eb.addField("Defesa", String.valueOf(defense), true);
			eb.addField("Força", String.valueOf(strength), true);

			return channel.sendMessage(eb.build());
		}
	}

	public static class Neck extends Item {
		private final int luck;
		private final int perception;

		public Neck(String name, String description, String image, int price, int luck, int perception) {
			super(name, description, image, Equipment.NECK, price);
			this.luck = luck;
			this.perception = perception;
		}

		public int getLuck() {
			return luck;
		}

		public int getPerception() {
			return perception;
		}

		@Override
		public RestAction info(TextChannel channel) {
			EmbedBuilder eb = infoModel();

			eb.addField("Sorte", String.valueOf(luck), true);
			eb.addField("Percepção", String.valueOf(perception), true);

			return channel.sendMessage(eb.build());
		}
	}

	public static class Bag extends Item {
		private final int capacity;

		public Bag(String name, String description, String image, int price, int capacity) {
			super(name, description, image, Equipment.BAG, price);
			this.capacity = capacity;
		}

		public int getCapacity() {
			return capacity;
		}

		@Override
		public RestAction info(TextChannel channel) {
			EmbedBuilder eb = infoModel();

			eb.addField("Capacidade", String.valueOf(capacity), true);

			return channel.sendMessage(eb.build());
		}
	}

	public static class Ring extends Item {
		private final int intelligence;
		private final int charisma;
		private final int luck;

		public Ring(String name, String description, String image, int price, int intelligence, int charisma, int luck) {
			super(name, description, image, Equipment.RING, price);
			this.intelligence = intelligence;
			this.charisma = charisma;
			this.luck = luck;
		}

		public int getIntelligence() {
			return intelligence;
		}

		public int getCharisma() {
			return charisma;
		}

		public int getLuck() {
			return luck;
		}

		@Override
		public RestAction info(TextChannel channel) {
			EmbedBuilder eb = infoModel();

			eb.addField("Inteligência", String.valueOf(intelligence), true);
			eb.addField("Carisma", String.valueOf(charisma), true);
			eb.addField("Sorte", String.valueOf(luck), true);

			return channel.sendMessage(eb.build());
		}
	}

	public static class Weapon extends Item {
		private final int strength;
		private final int intelligence;
		private final int defense;

		public Weapon(String name, String description, String image, int price, int strength, int intelligence, int defense) {
			super(name, description, image, Equipment.WEAPON, price);
			this.strength = strength;
			this.intelligence = intelligence;
			this.defense = defense;
		}

		public int getStrength() {
			return strength;
		}

		public int getIntelligence() {
			return intelligence;
		}

		public int getDefense() {
			return defense;
		}

		@Override
		public RestAction info(TextChannel channel) {
			EmbedBuilder eb = infoModel();

			eb.addField("Força", String.valueOf(strength), true);
			eb.addField("Inteligência", String.valueOf(intelligence), true);
			eb.addField("Defesa", String.valueOf(defense), true);

			return channel.sendMessage(eb.build());
		}
	}

	public static class Misc extends Item {
		public Misc(String name, String description, String image, int price) {
			super(name, description, image, Equipment.MISC, price);
		}

		@Override
		public RestAction info(TextChannel channel) {
			return channel.sendMessage(infoModel().build());
		}
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getImage() {
		return image;
	}

	public Equipment getType() {
		return type;
	}

	public float getPrice() {
		return price;
	}

	protected EmbedBuilder infoModel() {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle(name);
		eb.setDescription(description);
		eb.setThumbnail(image);
		eb.addField("Tipo", type.getName(), true);
		eb.addField("Valor", ":small_orange_diamond: " + price + " moeda" + (price != 0 ? "s" : ""), true);
		eb.addBlankField(false);

		return eb;
	}

	public abstract RestAction info(TextChannel channel);

	public Item getThis() {
		return this;
	}
}
