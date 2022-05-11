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

package com.kuuhaku.model.persistent.shoukan;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.FrameColor;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.shiro.Card;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deck")
public class Deck extends DAO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "account_uid")
	@Fetch(FetchMode.JOIN)
	private Account account;

	@Column(name = "current", nullable = false)
	private boolean current;

	@ManyToMany
	@JoinTable(name = "deck_senshi",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "senshi_card_id", referencedColumnName = "card_id"))
	@Fetch(FetchMode.SUBSELECT)
	private List<Senshi> senshi = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "deck_evogear",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "evogear_card_id", referencedColumnName = "card_id"))
	@Fetch(FetchMode.SUBSELECT)
	private List<Evogear> evogear = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "deck_field",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "field_card_id", referencedColumnName = "card_id"))
	@Fetch(FetchMode.SUBSELECT)
	private List<Field> field = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "frame", nullable = false)
	private FrameColor frame = FrameColor.PINK;

	@ManyToOne
	@JoinColumn(name = "cover_id")
	@Fetch(FetchMode.JOIN)
	private Card cover;

	@Column(name = "use_foil", nullable = false)
	private boolean useFoil;

	public Deck() {
	}

	public Deck(Account account) {
		this.account = account;
	}

	public int getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}

	public List<Senshi> getSenshi() {
		return senshi;
	}

	public List<Evogear> getEvogear() {
		return evogear;
	}

	public List<Field> getField() {
		return field;
	}

	public FrameColor getFrame() {
		return frame;
	}

	public void setFrame(FrameColor frame) {
		this.frame = frame;
	}

	public Card getCover() {
		return cover;
	}

	public void setCover(Card cover) {
		this.cover = cover;
	}

	public boolean isUsingFoil() {
		return useFoil;
	}

	public void setUseFoil(boolean useFoil) {
		this.useFoil = useFoil;
	}
}
