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

package com.kuuhaku.model.persistent;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "deck")
public class Deck {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191)")
	private String name = "";

	@LazyCollection(LazyCollectionOption.FALSE)
	@ManyToMany
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<Champion> champions = new ArrayList<>();

	@LazyCollection(LazyCollectionOption.FALSE)
	@ManyToMany
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<Equipment> equipments = new ArrayList<>();

	@LazyCollection(LazyCollectionOption.FALSE)
	@ManyToMany
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<Field> fields = new ArrayList<>();

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String destinyDraw = "";

	public Deck() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Champion getChampion(Card card) {
		return champions.stream().filter(k -> k.getCard().equals(card)).findFirst().orElse(null);
	}

	public List<Champion> getChampions() {
		return champions;
	}

	public void setChampions(List<Champion> champions) {
		this.champions = champions;
	}

	public void addChampion(Champion champion) {
		this.champions.add(champion);
	}

	public void removeChampion(Champion champion) {
		this.champions.remove(champion);
	}

	public int getChampionCopies(Card card) {
		return (int) champions.stream().filter(k -> k.getCard().equals(card)).count();
	}

	public boolean hasInvalidChampionCopyCount() {
		return champions.stream().distinct().anyMatch(c -> Collections.frequency(champions, c) > getChampionMaxCopies());
	}

	public Equipment getEquipment(Card card) {
		return equipments.stream().filter(k -> k.getCard().equals(card)).findFirst().orElse(null);
	}

	public List<Equipment> getEquipments() {
		return equipments;
	}

	public int getEvoWeight() {
		return equipments.stream().mapToInt(e -> e.getWeight(this)).sum();
	}

	public void setEquipments(List<Equipment> equipments) {
		this.equipments = equipments;
	}

	public void addEquipment(Equipment equipment) {
		this.equipments.add(equipment);
	}

	public void addEquipments(List<Equipment> equipments) {
		this.equipments.addAll(equipments);
	}

	public void removeEquipment(Equipment equipment) {
		this.equipments.remove(equipment);
	}

	public void removeEquipments(List<Equipment> equipments) {
		this.equipments.removeAll(equipments);
	}

	public int getEquipmentCopies(Card card) {
		return (int) equipments.stream().filter(k -> k.getCard().equals(card)).count();
	}

	public boolean hasInvalidEquipmentCopyCount() {
		return equipments.stream().distinct().anyMatch(c -> Collections.frequency(equipments, c) > getEquipmentMaxCopies(c))
			   || equipments.stream().filter(c -> c.getTier() == 4).count() > getEquipmentMaxCopies(4);
	}

	public boolean hasTierFour() {
		return equipments.stream().anyMatch(c -> c.getTier() == 4);
	}

	public Field getField(Card card) {
		return fields.stream().filter(k -> k.getCard().equals(card)).findFirst().orElse(null);
	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

	public void addField(Field field) {
		this.fields.add(field);
	}

	public void addFields(List<Field> fields) {
		this.fields.addAll(fields);
	}

	public void removeField(Field field) {
		this.fields.remove(field);
	}

	public void removeFields(List<Field> fields) {
		this.fields.removeAll(fields);
	}

	public int getFieldCopies(Card card) {
		return (int) fields.stream().filter(k -> k.getCard().equals(card)).count();
	}

	public List<Drawable> getDrawables() {
		return ListUtils.union(ListUtils.union(champions, equipments), fields);
	}

	public List<Integer> getDestinyDraw() {
		if (destinyDraw.isBlank()) return List.of();
		return Arrays.stream(destinyDraw.split(",")).map(Integer::parseInt).collect(Collectors.toList());
	}

	public void setDestinyDraw(List<Integer> destinyDraw) {
		if (destinyDraw == null)
			this.destinyDraw = "";
		else
			this.destinyDraw = destinyDraw.stream()
					.map(String::valueOf)
					.collect(Collectors.joining(","));
	}

	public Pair<Race, Race> getCombo() {
		return Race.getCombo(champions);
	}

	public int getChampionMaxCopies() {
		return getCombo().getLeft() == Race.HUMAN ? 4 : 3;
	}

	public int getEquipmentMaxCopies(Equipment eq) {
		if (eq == null) return 0;
		return 5 - eq.getTier() + (getCombo().getLeft() == Race.BESTIAL ? 1 : 0);
	}

	public int getEquipmentMaxCopies(int tier) {
		return 5 - tier + (getCombo().getLeft() == Race.BESTIAL ? 1 : 0);
	}

	public boolean checkChampion(Champion c, TextChannel channel) {
		int max = getChampionMaxCopies();
		if (c == null || c.isFusion()) {
			channel.sendMessage("❌ | Essa carta não é elegível para conversão.").queue();
			return true;
		} else if (Collections.frequency(getChampions(), c) >= max) {
			channel.sendMessage("❌ | Você só pode ter no máximo " + max + " cópias de cada campeão no deck.").queue();
			return true;
		} else if (getChampions().size() >= 36) {
			channel.sendMessage("❌ | Você só pode ter no máximo 36 campeões no deck.").queue();
			return true;
		}

		return false;
	}

	public static boolean checkChampion(Deck d, Champion c, TextChannel channel) {
		int max = d.getChampionMaxCopies();
		if (c == null || c.isFusion()) {
			channel.sendMessage("❌ | Essa carta não é elegível para conversão.").queue();
			return true;
		} else if (Collections.frequency(d.getChampions(), c) == max) {
			channel.sendMessage("❌ | Ele/Ela só pode ter no máximo " + max + " cópias de cada campeão no deck.").queue();
			return true;
		} else if (d.getChampions().size() >= 36) {
			channel.sendMessage("❌ | Ele/Ela só pode ter no máximo 36 campeões no deck.").queue();
			return true;
		}

		return false;
	}

	public int checkChampionError(Champion c) {
		if (c == null || c.isFusion()) {
			return 1;
		} else if (Collections.frequency(getChampions(), c) >= getChampionMaxCopies()) {
			return 2;
		} else if (getChampions().size() >= 36) {
			return 3;
		}

		return 0;
	}

	public static int checkChampionError(Deck d, Champion c) {
		if (c == null || c.isFusion()) {
			return 1;
		} else if (Collections.frequency(d.getChampions(), c) >= d.getChampionMaxCopies()) {
			return 2;
		} else if (d.getChampions().size() >= 36) {
			return 3;
		}

		return 0;
	}

	public boolean checkEquipment(Equipment e, TextChannel channel) {
		int max = getEquipmentMaxCopies(e);
		if (Collections.frequency(getEquipments(), e) >= max) {
			channel.sendMessage("❌ | Você só pode ter no máximo " + max + " cópias desse equipamento no deck.").queue();
			return true;
		} else if (getEquipments().stream().filter(eq -> eq.getTier() == 4).count() >= max) {
			channel.sendMessage("❌ | Você não possui mais espaços para evogears tier 4!").queue();
			return true;
		} else if (getEvoWeight() + e.getWeight(this) > 24) {
			channel.sendMessage("❌ | Você não possui mais espaços para evogears no deck.").queue();
			return true;
		}

		return false;
	}

	public static boolean checkEquipment(Deck d, Equipment e, TextChannel channel) {
		int max = d.getEquipmentMaxCopies(e);
		if (Collections.frequency(d.getEquipments(), e) >= max) {
			channel.sendMessage("❌ | Ele/Ela só pode ter no máximo " + max + " cópias desse equipamento no deck.").queue();
			return true;
		} else if (d.getEquipments().stream().filter(eq -> eq.getTier() == 4).count() >= max) {
			channel.sendMessage("❌ | Ele/Ela não possui mais espaços para evogears tier 4!").queue();
			return true;
		} else if (d.getEvoWeight() + e.getWeight(d) > 24) {
			channel.sendMessage("❌ | Ele/Ela não possui mais espaços para evogears no deck.").queue();
			return true;
		}

		return false;
	}

	public int checkEquipmentError(Equipment e) {
		int max = getEquipmentMaxCopies(e);
		if (Collections.frequency(getEquipments(), e) >= max) {
			return 1;
		} else if (getEquipments().stream().filter(eq -> eq.getTier() == 4).count() >= max) {
			return 2;
		} else if (getEvoWeight() + e.getWeight(this) > 24) {
			return 3;
		}

		return 0;
	}

	public static int checkEquipmentError(Deck d, Equipment e) {
		int max = d.getEquipmentMaxCopies(e);
		if (Collections.frequency(d.getEquipments(), e) >= max) {
			return 1;
		} else if (d.getEquipments().stream().filter(eq -> eq.getTier() == 4).count() >= max) {
			return 2;
		} else if (d.getEvoWeight() + e.getWeight(d) > 24) {
			return 3;
		}

		return 0;
	}

	public boolean checkField(Field f, TextChannel channel) {
		if (Collections.frequency(getFields(), f) >= 3) {
			channel.sendMessage("❌ | Você só pode ter no máximo 3 cópias de cada campo no deck.").queue();
			return true;
		} else if (getFields().size() >= 3) {
			channel.sendMessage("❌ | Você só pode ter no máximo 3 cartas de campo no deck.").queue();
			return true;
		}

		return false;
	}

	public static boolean checkField(Deck d, Field f, TextChannel channel) {
		if (Collections.frequency(d.getFields(), f) >= 3) {
			channel.sendMessage("❌ | Ele/Ela só pode ter no máximo 3 cópias de cada campo no deck.").queue();
			return true;
		} else if (d.getFields().size() >= 3) {
			channel.sendMessage("❌ | Ele/Ela só pode ter no máximo 3 cartas de campo no deck.").queue();
			return true;
		}

		return false;
	}

	public int checkFieldError(Field f) {
		if (Collections.frequency(getFields(), f) >= 3) {
			return 1;
		} else if (getFields().size() >= 3) {
			return 2;
		}

		return 0;
	}

	public static int checkFieldError(Deck d, Field f) {
		if (Collections.frequency(d.getFields(), f) >= 3) {
			return 1;
		} else if (d.getFields().size() >= 3) {
			return 2;
		}

		return 0;
	}

	public boolean hasInvalidDeck(TextChannel chn) {
		if (getChampions().size() < 30) {
			chn.sendMessage("❌ | É necessário ter ao menos 30 cartas no deck para poder jogar Shoukan.").queue();
			return true;
		} else if (getEvoWeight() > 24) {
			chn.sendMessage("❌ | Seus equipamentos ultrapassam a soma total de slots permitidos, remova alguns antes de poder jogar.").queue();
			return true;
		} else if (hasInvalidChampionCopyCount()) {
			chn.sendMessage("❌ | Seus campeões ultrapassam o limite máximo de cópias permitidas, remova alguns antes de poder jogar.").queue();
			return true;
		} else if (hasInvalidEquipmentCopyCount()) {
			chn.sendMessage("❌ | Seus equipamentos ultrapassam o limite máximo de cópias permitidas, remova alguns antes de poder jogar.").queue();
			return true;
		}

		return false;
	}

	public static boolean hasInvalidDeck(Deck d, TextChannel chn) {
		if (d.getChampions().size() < 30) {
			chn.sendMessage("❌ | Ele/ela não possui cartas suficientes, é necessário ter ao menos 30 cartas para poder jogar Shoukan.").queue();
			return true;
		} else if (d.getEvoWeight() > 24) {
			chn.sendMessage("❌ | Os equipamentos dele/dela ultrapassam a soma total de slots permitidos, remova alguns antes de poder jogar.").queue();
			return true;
		} else if (d.hasInvalidChampionCopyCount()) {
			chn.sendMessage("❌ | Os campeões dele/dela ultrapassam o limite máximo de cópias permitidas, remova alguns antes de poder jogar.").queue();
			return true;
		} else if (d.hasInvalidEquipmentCopyCount()) {
			chn.sendMessage("❌ | Os equipamentos dele/dela ultrapassam o limite máximo de cópias permitidas, remova alguns antes de poder jogar.").queue();
			return true;
		}

		return false;
	}

	public static boolean hasInvalidDeck(Deck d, User u, TextChannel chn) {
		if (d.getChampions().size() < 30) {
			chn.sendMessage("❌ | " + u.getAsMention() + " não possui cartas suficientes, é necessário ter ao menos 30 cartas para poder jogar Shoukan.").queue();
			return true;
		} else if (d.getEvoWeight() > 24) {
			chn.sendMessage("❌ | Os equipamentos de " + u.getAsMention() + " ultrapassam a soma total de slots permitidos, remova alguns antes de poder jogar.").queue();
			return true;
		} else if (d.hasInvalidChampionCopyCount()) {
			chn.sendMessage("❌ | Os campeões de " + u.getAsMention() + " ultrapassam o limite máximo de cópias permitidas, remova alguns antes de poder jogar.").queue();
			return true;
		} else if (d.hasInvalidEquipmentCopyCount()) {
			chn.sendMessage("❌ | Os equipamentos de " + u.getAsMention() + " ultrapassam o limite máximo de cópias permitidas, remova alguns antes de poder jogar.").queue();
			return true;
		}

		return false;
	}
}
