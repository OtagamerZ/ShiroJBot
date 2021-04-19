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

package com.kuuhaku.model.common;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.XStringBuilder;

import java.util.*;
import java.util.stream.Stream;

public class TradeContent {
	private final String uid;
	private final Set<KawaiponCard> cards = new HashSet<>();
	private final List<Equipment> equipments = new ArrayList<>();
	private final List<Field> fields = new ArrayList<>();
	private int credits = 0;
	private boolean closed = false;

	public TradeContent(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public Set<KawaiponCard> getCards() {
		return cards;
	}

	public List<Equipment> getEquipments() {
		return equipments;
	}

	public List<Field> getFields() {
		return fields;
	}

	public int getCredits() {
		return credits;
	}

	public void setCredits(int credits) {
		this.credits = credits;
	}

	public int getValue() {
		return Stream.of(cards, equipments, fields)
				.flatMap(Collection::stream)
				.mapToInt(o -> {
					if (o instanceof KawaiponCard) {
						KawaiponCard kc = (KawaiponCard) o;
						return kc.getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE / 2 * (kc.isFoil() ? 2 : 1);
					} else if (o instanceof Equipment) {
						Equipment e = (Equipment) o;
						return e.getTier() * Helper.BASE_EQUIPMENT_PRICE / 2;
					} else return Helper.BASE_FIELD_PRICE / 2;
				}).sum();
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean canReceive(Kawaipon kp) {
		Kawaipon aux = kp.clone();
		if (aux.getCards().stream().anyMatch(cards::contains)) return false;

		for (Equipment equipment : new HashSet<>(equipments)) {
			if (aux.checkEquipmentError(equipment) == 0) aux.addEquipment(equipment);
			else return false;
		}

		for (Field field : new HashSet<>(fields)) {
			if (aux.checkFieldError(field) == 0) aux.addField(field);
			else return false;
		}

		return true;
	}

	public static boolean isValidTrade(Collection<TradeContent> offers) {
		List<TradeContent> off = new ArrayList<>(offers);
		TradeContent off1 = off.get(0);
		TradeContent off2 = off.get(1);

		Account acc1 = AccountDAO.getAccount(off1.uid);
		Account acc2 = AccountDAO.getAccount(off2.uid);

		return (off1.credits + off1.getValue()) >= off2.getValue() * (acc2.getLoan() > 0 ? 4 : 1)
			   && (off2.credits + off2.getValue()) >= off1.getValue() * (acc1.getLoan() > 0 ? 4 : 1);
	}

	@Override
	public String toString() {
		XStringBuilder sb = new XStringBuilder("```css");

		if (credits + cards.size() + equipments.size() + fields.size() == 0) {
			sb.appendNewLine("Nada");
		} else {
			if (credits > 0) sb.appendNewLine(Helper.separate(credits) + " .CR");

			if (cards.size() > 0) sb.appendNewLine("[ Kawaipon ]");
			for (KawaiponCard card : cards) {
				sb.appendIndentNewLine(card.getName(), 1);
			}

			if (cards.size() > 0 && equipments.size() > 0) sb.appendNewLine("");
			if (equipments.size() > 0) sb.appendNewLine("[ Evogear ]");
			for (Equipment equipment : equipments) {
				sb.appendIndentNewLine(equipment.getCard().getName(), 1);
			}

			if ((cards.size() > 0 || equipments.size() > 0) && fields.size() > 0) sb.appendNewLine("");
			if (fields.size() > 0) sb.appendNewLine("[ Campo ]");
			for (Field field : fields) {
				sb.appendIndentNewLine(field.getCard().getName(), 1);
			}
		}

		return sb.appendNewLine("```").toString();
	}
}
