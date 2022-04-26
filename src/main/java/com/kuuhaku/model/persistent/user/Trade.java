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
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.XStringBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "trade")
public class Trade extends DAO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "left_uid")
	@Fetch(FetchMode.JOIN)
	private Account left;

	@Column(name = "left_value", nullable = false)
	private int leftValue;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "left_id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private List<TradeOffer> leftOffers = new ArrayList<>();

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "right_uid")
	@Fetch(FetchMode.JOIN)
	private Account right;

	@Column(name = "right_value", nullable = false)
	private int rightValue;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "right_id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private List<TradeOffer> rightOffers = new ArrayList<>();

	@Column(name = "closed", nullable = false)
	private boolean closed;

	public Trade() {
	}

	public Trade(String left, String right) {
		this.left = DAO.find(Account.class, left);
		this.right = DAO.find(Account.class, right);
	}

	public int getId() {
		return id;
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

	public List<TradeOffer> getLeftOffers() {
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

	public List<TradeOffer> getRightOffers() {
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

	public List<TradeOffer> getSelfOffers(String id) {
		return left.getUid().equals(id) ? leftOffers : rightOffers;
	}

	public void accept() {
		left.addCR(rightValue, "Trade Nº" + id + " commit");
		DAO.applyNative("""
				UPDATE stashed_card sc
				SET stash_uid = of.left_uid
				  , trade_id = NULL
				FROM (SELECT * FROM trade t INNER JOIN trade_offer of ON of.right_id = t.id) of
				WHERE of.id = ?1
				""", id);

		right.addCR(leftValue, "Trade Nº" + id + " commit");
		DAO.applyNative("""
				UPDATE stashed_card sc
				SET stash_uid = of.right_uid
				  , trade_id = NULL
				FROM (SELECT * FROM trade t INNER JOIN trade_offer of ON of.left_id = t.id) of
				WHERE of.id = ?1
				""", id);

		closed = true;
		save();
	}

	public void cancel() {
		left.addCR(leftValue, "Trade Nº" + id + " rollback");
		DAO.applyNative("""
				UPDATE stashed_card sc
				SET trade_id = NULL
				FROM (SELECT * FROM trade t INNER JOIN trade_offer of ON of.left_id = t.id) of
				WHERE of.id = ?1
				""", id);

		right.addCR(rightValue, "Trade Nº" + id + " rollback");
		DAO.applyNative("""
				UPDATE stashed_card sc
				SET trade_id = NULL
				FROM (SELECT * FROM trade t INNER JOIN trade_offer of ON of.right_id = t.id) of
				WHERE of.id = ?1
				""", id);

		closed = true;
		save();
	}

	public String toString(I18N locale, boolean left) {
		int value;
		List<TradeOffer> offers;
		if (left) {
			value = this.leftValue;
			offers = this.leftOffers;
		} else {
			value = this.rightValue;
			offers = this.rightOffers;
		}

		XStringBuilder sb = new XStringBuilder("```asciidoc");
		sb.appendNewLine("= " + Utils.separate(value, locale.getLocale()) + " ₵R =");

		offers.sort(Comparator.comparing(TradeOffer::getType));
		CardType type = null;
		for (TradeOffer offer : offers) {
			if (type != offer.getType()) {
				type = offer.getType();
				sb.appendNewLine("\n[" + locale.get("type/" + type.name()) + "]");
			}

			sb.appendNewLine("- " + DAO.query(type.getKlass(), type.getQuery(), offer.getUUID()));
		}

		sb.appendNewLine("```");

		return sb.toString();
	}
}
