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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "kawaipon_card")
public class KawaiponCard extends DAO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "uuid", nullable = false, unique = true, length = 36)
	private String uuid = UUID.randomUUID().toString();

	@Column(name = "foil", nullable = false)
	private boolean foil;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "card_id")
	private Card card;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "kawaipon_uid")
	@Fetch(FetchMode.JOIN)
	private Kawaipon kawaipon;

	public KawaiponCard() {
	}

	public KawaiponCard(Kawaipon kawaipon, Card card, boolean foil) {
		this.kawaipon = kawaipon;
		this.card = card;
		this.foil = foil;
	}

	public int getId() {
		return id;
	}

	public String getUuid() {
		return uuid;
	}

	public boolean isFoil() {
		return foil;
	}

	public void setFoil(boolean foil) {
		this.foil = foil;
	}

	public Card getCard() {
		return card;
	}

	public Kawaipon getKawaipon() {
		return kawaipon;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KawaiponCard that = (KawaiponCard) o;
		return foil == that.foil && Objects.equals(card, that.card);
	}

	@Override
	public int hashCode() {
		return Objects.hash(foil, card);
	}
}
