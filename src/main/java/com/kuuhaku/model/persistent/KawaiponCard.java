/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Entity
@Table(name = "kawaiponcard")
public class KawaiponCard {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "kawaipon_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Kawaipon kawaipon;

	@ManyToOne(fetch = FetchType.EAGER)
	private Card card;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean foil = false;

	@Column(unique = true, columnDefinition = "CHAR(64) NOT NULL")
	private String hash = Helper.hash(
			Helper.generateToken(String.valueOf(System.currentTimeMillis() * Math.random()), 256)
					.getBytes(StandardCharsets.UTF_8), "SHA-256");

	public KawaiponCard(Card card, boolean foil) {
		this.card = card;
		this.foil = foil;
	}

	public KawaiponCard() {
	}

	public String getName() {
		return (foil ? "« " : "") + card.getName() + (foil ? " »" : "");
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Kawaipon getKawaipon() {
		return kawaipon;
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}

	public boolean isFoil() {
		return foil;
	}

	public void setFoil(boolean foil) {
		this.foil = foil;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
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
		return Objects.hash(card, foil);
	}

	@Override
	public String toString() {
		return new JSONObject(card.toString()) {{
			put("foil", foil);
			put("hash", hash);
		}}.toString();
	}

	public String getBase64() {
		return Helper.atob(card.drawCard(foil), "png");
	}
}
