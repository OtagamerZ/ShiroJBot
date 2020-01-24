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

import com.kuuhaku.handlers.games.rpg.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
public class Mob extends Character{
	private final List<LootItem> lootTable;
	private final int xp;

	public Mob(String name, String img, String bio, int strength, int perception, int endurance, int charisma, int intelligence, int agility, int luck, int xp) {
		super(name, img, bio, strength, perception, endurance, charisma, intelligence, agility, luck);
		this.lootTable = new ArrayList<>();
		this.xp = xp;
	}

	public Mob(String name, String img, String bio, int strength, int perception, int endurance, int charisma, int intelligence, int agility, int xp, int luck, List<LootItem> loot) {
		super(name, img, bio, strength, perception, endurance, charisma, intelligence, agility, luck);
		this.lootTable = loot;
		this.xp = xp;
	}

	public Mob(String[] ssvData, int xp) {
		super(ssvData);
		this.lootTable = new ArrayList<>();
		this.xp = xp;
	}

	@Override
	public RestAction openProfile(TextChannel channel) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle(super.getName());
		eb.setThumbnail(super.getImage());
		eb.setDescription(super.getBio());
		eb.addField("Vida", String.valueOf(super.getStatus().getLife()), false);
		eb.addField("Defesa", String.valueOf(super.getStatus().getDefense()), true);
		eb.addField("Força", String.valueOf(super.getStatus().getStrength()), true);
		eb.addField("Percepção", String.valueOf(super.getStatus().getPerception()), true);
		eb.addField("Resistência", String.valueOf(super.getStatus().getEndurance()), true);
		eb.addField("Carisma", String.valueOf(super.getStatus().getCharisma()), true);
		eb.addField("Inteligência", String.valueOf(super.getStatus().getIntelligence()), true);
		eb.addField("Agilidade", String.valueOf(super.getStatus().getAgility()), true);
		eb.addField("Sorte", String.valueOf(super.getStatus().getLuck()), true);
		eb.addBlankField(false);
		eb.addField("XP", String.valueOf(xp), true);
		eb.addField("Drops", lootTable.stream().map(i -> "(" + i.getItem().getType().getName() + " " + i.getRarity().getName() + ") " +i.getItem().getName() + "\n").collect(Collectors.joining()), false);

		return channel.sendMessage(eb.build());
	}

	public void addLoot(LootItem item) {
		lootTable.add(item);
	}

	public void removeLoot(LootItem item) {
		lootTable.remove(item);
	}

	public List<LootItem> getLootTable() {
		return lootTable;
	}

	public Item dropLoot(int luck) {
		return Utils.getItem(luck, lootTable);
	}

	public int getXp() {
		return xp;
	}
}
