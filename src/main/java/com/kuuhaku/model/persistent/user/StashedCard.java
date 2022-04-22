/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.persistent.shiro.Card;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "stashed_card")
public class StashedCard extends DAO {
	@Id
	@Column(name = "uuid", nullable = false, length = 36)
	private String uuid = UUID.randomUUID().toString();

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	private Card card;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private CardType type;

	@Column(name = "foil", nullable = false)
	private boolean foil;

	@Column(name = "quality", nullable = false)
	private double quality;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "stash_uid")
	@Fetch(FetchMode.JOIN)
	private Stash stash;

	public StashedCard() {

	}

	public StashedCard(Stash stash, Card card, CardType type, double quality, boolean foil) {
		this.card = card;
		this.type = type;
		this.foil = foil;
		this.quality = quality;
		this.stash = stash;
	}

	public StashedCard(Stash stash, String uuid, Card card, CardType type, double quality, boolean foil) {
		this.uuid = uuid;
		this.card = card;
		this.type = type;
		this.foil = foil;
		this.quality = quality;
		this.stash = stash;
	}

	public String getUUID() {
		return uuid;
	}

	public Card getCard() {
		return card;
	}

	public CardType getType() {
		return type;
	}

	public boolean isFoil() {
		return foil;
	}

	public double getQuality() {
		return quality;
	}

	public Stash getStash() {
		return stash;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StashedCard that = (StashedCard) o;
		return Objects.equals(uuid, that.uuid) && type == that.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid, type);
	}
}
