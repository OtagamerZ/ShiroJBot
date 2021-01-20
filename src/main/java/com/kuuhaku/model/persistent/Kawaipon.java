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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import org.apache.commons.collections4.ListUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "kawaipon")
public class Kawaipon {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''", unique = true)
	private String uid = "";

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private Set<KawaiponCard> cards = new HashSet<>();

	@LazyCollection(LazyCollectionOption.FALSE)
	@ManyToMany
	private List<Champion> champions = new ArrayList<>();

	@LazyCollection(LazyCollectionOption.FALSE)
	@ManyToMany
	private List<Equipment> equipments = new ArrayList<>();

	@LazyCollection(LazyCollectionOption.FALSE)
	@ManyToMany
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

	public Equipment getEquipment(Card card) {
		return equipments.stream().filter(k -> k.getCard().equals(card)).findFirst().orElse(null);
	}

	public List<Equipment> getEquipments() {
		return equipments;
	}

	public void setEquipments(List<Equipment> equipments) {
		this.equipments = equipments;
	}

	public void addEquipment(Equipment equipment) {
		this.equipments.add(equipment);
	}

	public void removeEquipment(Equipment equipment) {
		this.equipments.remove(equipment);
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

	public void removeField(Field field) {
		this.fields.remove(field);
	}

	public Field getField(Card card) {
		return fields.stream().filter(k -> k.getCard().equals(card)).findFirst().orElse(null);
	}

	public List<Drawable> getDrawables() {
		return ListUtils.union(ListUtils.union(champions, equipments), fields);
	}

	public int getCollectionHash() {
		List<String> cards = Stream.of(this.cards, champions)
				.flatMap(Collection::stream)
				.map(o -> {
					if (o instanceof KawaiponCard) return ((KawaiponCard) o).getCard();
					else return ((Champion) o).getCard();
				})
				.map(Card::getId)
				.sorted(String::compareTo)
				.collect(Collectors.toList());

		return cards.hashCode();
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
}
