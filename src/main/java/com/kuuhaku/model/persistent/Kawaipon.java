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
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "kawaipon")
public class Kawaipon implements Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''", unique = true)
	private String uid = "";

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "kawaipon_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<KawaiponCard> cards = new HashSet<>();

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

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public KawaiponCard getCard(Card card, boolean foil) {
		return cards.stream().filter(k -> k.getCard().equals(card) && k.isFoil() == foil).findFirst().orElse(null);
	}

	public Set<KawaiponCard> getCards() {
		return cards;
	}

	public void setCards(Set<KawaiponCard> cards) {
		this.cards = cards;
	}

	public void addCards(Set<KawaiponCard> cards) {
		this.cards.removeIf(kc -> !kc.isFoil());
		this.cards.addAll(cards);
	}

	public void addCard(KawaiponCard card) {
		this.cards.add(card);
	}

	public void removeCard(KawaiponCard card) {
		this.cards.remove(card);
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

	public int getEquipmentCopies(Card card) {
		return (int) equipments.stream().filter(k -> k.getCard().equals(card)).count();
	}

	public boolean hasInvalidEquipmentCopyCount() {
		return equipments.stream().distinct().anyMatch(c -> Collections.frequency(equipments, c) > getEquipmentMaxCopies(c))
			   || equipments.stream().filter(c -> c.getTier() == 4).count() > getEquipmentMaxCopies(true);
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

	public int getFieldCopies(Card card) {
		return (int) fields.stream().filter(k -> k.getCard().equals(card)).count();
	}

	public List<Drawable> getDrawables() {
		return ListUtils.union(ListUtils.union(champions, equipments), fields);
	}

	public List<Integer> getDestinyDraw() {
		if (destinyDraw.isBlank()) return null;
		return Arrays.stream(destinyDraw.split(",")).map(Integer::parseInt).collect(Collectors.toList());
	}

	public void setDestinyDraw(Integer[] destinyDraw) {
		if (destinyDraw == null)
			this.destinyDraw = "";
		else
			this.destinyDraw = Arrays.stream(destinyDraw).map(String::valueOf).collect(Collectors.joining(","));
	}

	public Pair<Race, Race> getCombo() {
		return Race.getCombo(champions);
	}

	public int getChampionMaxCopies() {
		return getCombo().getLeft() == Race.HUMAN ? 4 : 3;
	}

	public int getEquipmentMaxCopies(Equipment eq) {
		if (eq == null) return 0;
		else if (eq.getTier() == 4) {
			return getCombo().getLeft() == Race.BESTIAL ? 2 : 1;
		} else {
			return getCombo().getLeft() == Race.BESTIAL ? 4 : 3;
		}
	}

	public int getEquipmentMaxCopies(boolean mythic) {
		if (mythic) {
			return getCombo().getLeft() == Race.BESTIAL ? 2 : 1;
		} else {
			return getCombo().getLeft() == Race.BESTIAL ? 4 : 3;
		}
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

	public static boolean checkChampion(Kawaipon kp, Champion c, TextChannel channel) {
		int max = kp.getChampionMaxCopies();
		if (c == null || c.isFusion()) {
			channel.sendMessage("❌ | Essa carta não é elegível para conversão.").queue();
			return true;
		} else if (Collections.frequency(kp.getChampions(), c) == max) {
			channel.sendMessage("❌ | Ele/Ela só pode ter no máximo " + max + " cópias de cada campeão no deck.").queue();
			return true;
		} else if (kp.getChampions().size() >= 36) {
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

	public static int checkChampionError(Kawaipon kp, Champion c) {
		if (c == null || c.isFusion()) {
			return 1;
		} else if (Collections.frequency(kp.getChampions(), c) >= kp.getChampionMaxCopies()) {
			return 2;
		} else if (kp.getChampions().size() >= 36) {
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

	public static boolean checkEquipment(Kawaipon kp, Equipment e, TextChannel channel) {
		int max = kp.getEquipmentMaxCopies(e);
		if (Collections.frequency(kp.getEquipments(), e) >= max) {
			channel.sendMessage("❌ | Ele/Ela só pode ter no máximo " + max + " cópias desse equipamento no deck.").queue();
			return true;
		} else if (kp.getEquipments().stream().filter(eq -> eq.getTier() == 4).count() >= max) {
			channel.sendMessage("❌ | Ele/Ela não possui mais espaços para evogears tier 4!").queue();
			return true;
		} else if (kp.getEvoWeight() + e.getWeight(kp) > 24) {
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

	public static int checkEquipmentError(Kawaipon kp, Equipment e) {
		int max = kp.getEquipmentMaxCopies(e);
		if (Collections.frequency(kp.getEquipments(), e) >= max) {
			return 1;
		} else if (kp.getEquipments().stream().filter(eq -> eq.getTier() == 4).count() >= max) {
			return 2;
		} else if (kp.getEvoWeight() + e.getWeight(kp) > 24) {
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

	public static boolean checkField(Kawaipon kp, Field f, TextChannel channel) {
		if (Collections.frequency(kp.getFields(), f) >= 3) {
			channel.sendMessage("❌ | Ele/Ela só pode ter no máximo 3 cópias de cada campo no deck.").queue();
			return true;
		} else if (kp.getFields().size() >= 3) {
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

	public static int checkFieldError(Kawaipon kp, Field f) {
		if (Collections.frequency(kp.getFields(), f) >= 3) {
			return 1;
		} else if (kp.getFields().size() >= 3) {
			return 2;
		}

		return 0;
	}

	public Kawaipon copy() {
		try {
			return (Kawaipon) super.clone();
		} catch (CloneNotSupportedException e) {
			return new Kawaipon();
		}
	}
}
