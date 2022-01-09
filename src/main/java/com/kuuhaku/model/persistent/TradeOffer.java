/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.XStringBuilder;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tradeoffer")
public class TradeOffer {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "trade_id")
	private Trade trade;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "tradeoffer_id")
	private List<TradeCard> cards = new ArrayList<>();

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int value = 0;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean accepted = false;

	public TradeOffer() {
	}

	public TradeOffer(String uid, Trade trade) {
		this.uid = uid;
		this.trade = trade;
	}

	public String getUid() {
		return uid;
	}

	public Trade getTrade() {
		return trade;
	}

	public List<TradeCard> getCards() {
		return cards;
	}

	public int getValue() {
		return value;
	}

	public void addValue(int value) {
		this.value += value;
	}

	public void removeValue(int value) {
		this.value -= value;
	}

	public Kawaipon getKawaipon() {
		return KawaiponDAO.getKawaipon(uid);
	}

	public boolean hasAccepted() {
		return accepted;
	}

	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}

	public Account getAccount() {
		return AccountDAO.getAccount(uid);
	}

	public void rollback() {
		Account acc = getAccount();
		acc.addCredit(value, this.getClass());
		AccountDAO.saveAccount(acc);

		Kawaipon kp = getKawaipon();
		Deck dk = kp.getDeck();

		for (TradeCard card : cards) {
			switch (card.getType()) {
				case KAWAIPON -> {
					KawaiponCard kc = new KawaiponCard(card.getCard(), card.isFoil());

					if (kp.getCards().contains(kc)) {
						StashDAO.saveCard(new Stash(uid, kc));
					} else {
						kp.addCard(kc);
					}
				}
				case SENSHI -> {
					Champion c = CardDAO.getChampion(card.getCard());

					if (dk.checkChampionError(c) != 0) {
						StashDAO.saveCard(new Stash(uid, c));
					} else {
						dk.addChampion(c);
					}
				}
				case EVOGEAR -> {
					Equipment e = CardDAO.getEquipment(card.getCard());

					if (dk.checkEquipmentError(e) != 0) {
						StashDAO.saveCard(new Stash(uid, e));
					} else {
						dk.addEquipment(e);
					}
				}
				case FIELD -> {
					Field f = CardDAO.getField(card.getCard());

					if (dk.checkFieldError(f) != 0) {
						StashDAO.saveCard(new Stash(uid, f));
					} else {
						dk.addField(f);
					}
				}
			}
		}

		KawaiponDAO.saveKawaipon(kp);
	}

	public void rollback(TradeCard card) {
		Kawaipon kp = getKawaipon();
		Deck dk = kp.getDeck();

		switch (card.getType()) {
			case KAWAIPON -> {
				KawaiponCard kc = new KawaiponCard(card.getCard(), card.isFoil());

				if (kp.getCards().contains(kc)) {
					StashDAO.saveCard(new Stash(uid, kc));
				} else {
					kp.addCard(kc);
				}
			}
			case SENSHI -> {
				Champion c = CardDAO.getChampion(card.getCard());

				if (dk.checkChampionError(c) != 0) {
					StashDAO.saveCard(new Stash(uid, c));
				} else {
					dk.addChampion(c);
				}
			}
			case EVOGEAR -> {
				Equipment e = CardDAO.getEquipment(card.getCard());

				if (dk.checkEquipmentError(e) != 0) {
					StashDAO.saveCard(new Stash(uid, e));
				} else {
					dk.addEquipment(e);
				}
			}
			case FIELD -> {
				Field f = CardDAO.getField(card.getCard());

				if (dk.checkFieldError(f) != 0) {
					StashDAO.saveCard(new Stash(uid, f));
				} else {
					dk.addField(f);
				}
			}
		}

		cards.remove(card);
		KawaiponDAO.saveKawaipon(kp);
	}

	public void commit(String uid) {
		TradeOffer to = new TradeOffer(uid, trade);
		to.addValue(value);
		to.getCards().addAll(cards);
		to.rollback();
	}

	@Override
	public String toString() {
		XStringBuilder sb = new XStringBuilder("```css");

		if (value + cards.size() == 0) {
			sb.appendNewLine("Nada");
		} else {
			if (value > 0) {
				sb.appendNewLine(Helper.separate(value) + " CR");
			}

			String head = "";
			for (TradeCard card : cards) {
				String capt = switch (card.getType()) {
					case KAWAIPON -> "[ Kawaipon ]";
					case SENSHI -> "[ Senshi ]";
					case EVOGEAR -> "[ Evogear ]";
					case FIELD -> "[ Campo ]";
					case NONE -> "";
				};

				if (!head.equals(capt)) {
					if (!head.isBlank()) {
						sb.appendNewLine("");
					}

					sb.appendNewLine(capt);
					head = capt;
				}

				sb.appendIndentNewLine(card.getCard().getName(), 1);
			}
		}

		return sb.appendNewLine("```").toString();
	}
}
