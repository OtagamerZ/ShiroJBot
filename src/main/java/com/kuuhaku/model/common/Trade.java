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

package com.kuuhaku.model.common;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;
import org.intellij.lang.annotations.Language;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Trade {
	private static final MultiMap<String, Trade> pending = new MultiMap<>(ConcurrentHashMap::new);

	private Account left;
	private int leftValue;
	private List<Integer> leftOffers = new ArrayList<>();

	private Account right;
	private int rightValue;
	private List<Integer> rightOffers = new ArrayList<>();

	private boolean finalizing = false;

	public Trade(String left, String right) {
		this.left = DAO.find(Account.class, left);
		this.right = DAO.find(Account.class, right);
	}

	public static MultiMap<String, Trade> getPending() {
		return pending;
	}

	public Account getLeft() {
		return left;
	}

	public int getLeftValue() {
		return leftValue;
	}

	public void addLeftValue(int value) {
		leftValue += value;
	}

	public List<Integer> getLeftOffers() {
		return leftOffers;
	}

	public Account getRight() {
		return right;
	}

	public int getRightValue() {
		return rightValue;
	}

	public void addRightValue(int value) {
		rightValue += value;
	}

	public List<Integer> getRightOffers() {
		return rightOffers;
	}

	public Account getSelf(String id) {
		return left.getUid().equals(id) ? left : right;
	}

	public int getSelfValue(String id) {
		return left.getUid().equals(id) ? leftValue : rightValue;
	}

	public void addSelfValue(String id, int value) {
		if (left.getUid().equals(id)) {
			leftValue += value;
		} else {
			rightValue += value;
		}
	}

	public List<Integer> getSelfOffers(String id) {
		return left.getUid().equals(id) ? leftOffers : rightOffers;
	}

	public boolean validate() {
		if (!left.hasEnough(leftValue, Currency.CR) || right.hasEnough(rightValue, Currency.CR)) return false;

		@Language("PostgreSQL")
		String query = """
				SELECT COUNT(1)
				FROM stashed_card sc
				WHERE sc.kawaipon_uid = ?1
				AND sc.id IN ?2
				AND sc.deck_id IS NULL
				AND sc.price = 0
				""";

		if (DAO.queryNative(Integer.class, query, left.getUid(), leftOffers) != leftOffers.size()) {
			return false;
		}

		return DAO.queryNative(Integer.class, query, right.getUid(), rightOffers) == rightOffers.size();
	}

	public void accept() {
		left.addCR(rightValue, "Trade (" + left.getName() + "/" + right.getName() + ") commit");
		left.consumeCR(leftValue, "Trade (" + left.getName() + "/" + right.getName() + ") commit");
		DAO.apply("""
				UPDATE StashedCard sc
				SET kawaipon = ?1
				WHERE sc.id IN ?2
				""", left.getKawaipon(), rightOffers);

		right.addCR(leftValue, "Trade (" + left.getName() + "/" + right.getName() + ") commit");
		right.consumeCR(rightValue, "Trade (" + left.getName() + "/" + right.getName() + ") commit");
		DAO.apply("""    
				UPDATE StashedCard sc
				SET kawaipon = ?1
				WHERE sc.id IN ?2
				""", right.getKawaipon(), leftOffers);
	}

	public String toString(I18N locale, boolean left) {
		int value;
		List<StashedCard> offers;
		if (left) {
			value = this.leftValue;
			offers = this.leftOffers.stream()
					.map(id -> DAO.find(StashedCard.class, id))
					.sorted(Comparator.comparing(StashedCard::getType))
					.toList();
		} else {
			value = this.rightValue;
			offers = this.rightOffers.stream()
					.map(id -> DAO.find(StashedCard.class, id))
					.sorted(Comparator.comparing(StashedCard::getType))
					.toList();
		}

		XStringBuilder sb = new XStringBuilder("```asciidoc");
		sb.appendNewLine("= " + Utils.separate(value) + " ₵R =");

		CardType type = null;
		for (StashedCard card : offers) {
			if (type != card.getType()) {
				type = card.getType();
				sb.appendNewLine("\n[" + locale.get("type/" + type.name()) + "]");
			}

			sb.appendNewLine("- " + card);
		}

		sb.appendNewLine("```");

		return sb.toString();
	}

	public boolean isFinalizing() {
		return finalizing;
	}

	public void setFinalizing(boolean finalizing) {
		this.finalizing = finalizing;
	}
}
