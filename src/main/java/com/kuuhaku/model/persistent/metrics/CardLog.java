/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.metrics;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Kawaipon;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "card_log")
public class CardLog extends DAO<CardLog> {
	public enum Source {
		SPAWN, GACHA, OTHER
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false)
	private Source source;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	private Card card;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@PrimaryKeyJoinColumn(name = "kawaipon_uid")
	@Fetch(FetchMode.JOIN)
	private Kawaipon kawaipon;

	@Column(name = "date", nullable = false)
	private ZonedDateTime date;

	public CardLog() {
	}

	public CardLog(Kawaipon kawaipon, Card card, Source source) {
		this.kawaipon = kawaipon;
		this.card = card;
		this.source = source;
	}

	public int getId() {
		return id;
	}

	public Source getSource() {
		return source;
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
		CardLog cardLog = (CardLog) o;
		return id == cardLog.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
