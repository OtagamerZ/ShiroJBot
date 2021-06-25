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

package com.kuuhaku.model.common;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.LotteryDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.XStringBuilder;

import java.util.*;
import java.util.stream.Stream;

public class TradeContent {
	private final String uid;
	private final Account acc;
	private final Kawaipon kp;
	private final Set<KawaiponCard> cards = new HashSet<>();
	private final List<Equipment> equipments = new ArrayList<>();
	private final List<Field> fields = new ArrayList<>();
	private int credits = 0;
	private boolean closed = false;

	public TradeContent(String uid) {
		this.uid = uid;
		this.acc = AccountDAO.getAccount(uid);
		this.kp = KawaiponDAO.getKawaipon(uid);
	}

	public String getUid() {
		return uid;
	}

	public Account getAcc() {
		return acc;
	}

	public Kawaipon getKp() {
		return kp;
	}

	public Deck getDk() {
		return kp.getDeck();
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
						   if (o instanceof KawaiponCard kc) {
							   return kc.getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE / 2 * (kc.isFoil() ? 2 : 1);
						   } else if (o instanceof Equipment) {
							   return Helper.BASE_EQUIPMENT_PRICE / 2;
						   } else return Helper.BASE_FIELD_PRICE / 2;
					   }).sum() * (getAccount().getLoan() > 0 ? 4 : 1);
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean canReceive(Kawaipon kp) {
		Deck dk = kp.getDeck();
		for (KawaiponCard card : cards) {
			if (kp.getCards().contains(card)) return false;
		}

		for (Equipment equipment : equipments) {
			if (dk.checkEquipmentError(equipment) == 0) dk.addEquipment(equipment);
			else return false;
		}

		for (Field field : fields) {
			if (dk.checkFieldError(field) == 0) dk.addField(field);
			else return false;
		}

		return true;
	}

	public Account getAccount() {
		return AccountDAO.getAccount(uid);
	}

	public Kawaipon getKawaipon() {
		return KawaiponDAO.getKawaipon(uid);
	}

	public Deck getDeck() {
		return KawaiponDAO.getKawaipon(uid).getDeck();
	}

	public static boolean isValidTrade(Collection<TradeContent> offers) {
		List<TradeContent> off = List.copyOf(offers);
		TradeContent off1 = off.get(0);
		TradeContent off2 = off.get(1);

		Account acc1 = off1.getAccount();
		Account acc2 = off2.getAccount();

		return (off1.credits + off1.getValue()) >= off2.getValue() * (acc2.getLoan() > 0 ? 4 : 1)
			   && (off2.credits + off2.getValue()) >= off1.getValue() * (acc1.getLoan() > 0 ? 4 : 1);
	}

	public static void trade(Collection<TradeContent> offers) {
		for (TradeContent tc : offers) {
			TradeContent other = offers.stream()
					.filter(t -> !tc.equals(t))
					.findFirst()
					.orElseThrow();

			Account acc = tc.getAccount();
			Kawaipon kp = tc.getKawaipon();
			Deck dk = kp.getDeck();

			int liquidAmount = Helper.applyTax(other.uid, other.credits, 0.1);
			acc.addCredit(liquidAmount, TradeContent.class);

			kp.addCards(other.cards);
			dk.addEquipments(other.equipments);
			dk.addFields(other.fields);

			Account oAcc = tc.getAccount();
			Kawaipon oKp = other.getKawaipon();
			Deck oDk = oKp.getDeck();

			oAcc.removeCredit(other.credits, TradeContent.class);

			oKp.removeCards(other.cards);
			oDk.removeEquipments(other.equipments);
			oDk.removeFields(other.fields);

			LotteryValue lv = LotteryDAO.getLotteryValue();
			lv.addValue((tc.credits - liquidAmount));
			LotteryDAO.saveLotteryValue(lv);

			KawaiponDAO.saveKawaipon(kp);
			AccountDAO.saveAccount(acc);

			KawaiponDAO.saveKawaipon(oKp);
			AccountDAO.saveAccount(oAcc);
		}
	}

	@Override
	public String toString() {
		XStringBuilder sb = new XStringBuilder("```css");

		if (credits + cards.size() + equipments.size() + fields.size() == 0) {
			sb.appendNewLine("Nada");
		} else {
			if (credits > 0) {
				int liquidAmount = Helper.applyTax(uid, credits, 0.1);
				boolean taxed = credits != liquidAmount;
				String taxMsg = taxed ? " (Taxa: " + Helper.roundToString(100 - Helper.prcnt(liquidAmount, credits) * 100, 1) + "%)" : "";
				sb.appendNewLine(Helper.separate(credits) + " .CR" + taxMsg);
			}

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TradeContent that = (TradeContent) o;
		return Objects.equals(uid, that.uid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid);
	}
}
