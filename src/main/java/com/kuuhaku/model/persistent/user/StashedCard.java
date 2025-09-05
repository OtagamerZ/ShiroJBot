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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.Market;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Quality;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shiro.GlobalProperty;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import okio.Buffer;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "stashed_card", schema = "kawaipon")
public class StashedCard extends DAO<StashedCard> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "uuid", nullable = false, unique = true, length = 36)
	private String uuid = UUID.randomUUID().toString();

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	private Card card;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private CardType type;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "kawaipon_uid")
	@Fetch(FetchMode.JOIN)
	private Kawaipon kawaipon;

	@ManyToOne
	@JoinColumn(name = "deck_id")
	private Deck deck;

	@Column(name = "price", nullable = false)
	private int price = 0;

	@Column(name = "locked", nullable = false)
	private boolean locked = false;

	@Column(name = "account_bound", nullable = false)
	private boolean accountBound = false;

	@Column(name = "in_collection", nullable = false)
	private boolean inCollection = false;

	@Column(name = "chrome", nullable = false)
	private boolean chrome;

	@Column(name = "quality", nullable = false)
	private double quality = rollQuality();

	public StashedCard() {
	}

	public StashedCard(Card card, boolean chrome) {
		this(null, card, chrome);
	}

	public StashedCard(Kawaipon kawaipon, Card card, boolean chrome) {
		this.card = card;
		this.chrome = chrome;
		this.type = CardType.KAWAIPON;
		this.kawaipon = kawaipon;
	}

	public StashedCard(Kawaipon kawaipon, Drawable<?> card) {
		this.card = card.getCard();
		this.kawaipon = kawaipon;

		if (card instanceof Senshi) {
			this.type = CardType.KAWAIPON;
		} else if (card instanceof Evogear) {
			this.type = CardType.EVOGEAR;
		} else {
			this.type = CardType.FIELD;
		}
	}

	public int getId() {
		return id;
	}

	public String getUUID() {
		return uuid;
	}

	public boolean isChrome() {
		return chrome;
	}

	public void setChrome(boolean chrome) {
		this.chrome = chrome;
	}

	public double getQuality() {
		return quality;
	}

	public void setQuality(double quality) {
		this.quality = quality;
	}

	public double rollQuality() {
		return Calc.round(Math.max(0, Math.pow(ThreadLocalRandom.current().nextDouble(), 5) * 40 - 20), 1);
	}

	public Card getCard() {
		return card;
	}

	public String getName() {
		return (isChrome() ? "« %s »" : "%s").formatted(card.getName());
	}

	public CardType getType() {
		return type;
	}

	public Kawaipon getKawaipon() {
		return kawaipon;
	}

	public void setKawaipon(Kawaipon kawaipon) {
		this.kawaipon = kawaipon;
	}

	public Deck getDeck() {
		return deck;
	}

	public void setDeck(Deck deck) {
		this.deck = deck;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		if (price == 0) {
			GlobalProperty gp = Utils.getOr(DAO.find(GlobalProperty.class, "daily_offer"), new GlobalProperty("daily_offer", "{}"));
			JSONObject dailyOffer = new JSONObject(gp.getValue());

			if (dailyOffer.getInt("id") == id) {
				dailyOffer.put("id", "-1");
				gp.setValue(dailyOffer);
				gp.save();
			}
		}

		this.price = price;
	}

	public int getCollectPrice() {
		return Math.max(0, card.getRarity().getIndex() * 200);
	}

	public int getSuggestedPrice() {
		return (int) (Math.max(1, card.getRarity().getIndex() * 500) * Math.pow(1.0025, Math.pow(getQuality(), 2)));
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public void lock() {
		this.locked = true;
	}

	public void unlock() {
		this.locked = false;
	}

	public boolean isAccountBound() {
		return accountBound;
	}

	public boolean isInCollection() {
		return inCollection;
	}

	public void setInCollection(boolean inCollection) {
		this.inCollection = inCollection;
	}

	public BufferedImage render() {
		BufferedImage bi = card.drawCard(isChrome());
		Quality q = Quality.get(getQuality());

		if (q.ordinal() > 0) {
			try {
				try (Buffer buf = new Buffer(); InputStream is = buf.inputStream()) {
					buf.write(q.getOverlayBytes());
					BufferedImage img = ImageIO.read(is);

					Graphics2D g2d = bi.createGraphics();
					g2d.setRenderingHints(Constants.HD_HINTS);

					if (isChrome()) {
						card.chrome(img, true);
					}

					g2d.drawImage(img, 0, 0, null);
					g2d.dispose();
				}
			} catch (IOException e) {
				throw new RuntimeException("Error when generating card", e);
			}
		}

		return bi;
	}

	@Override
	public void afterSave() {
		if (price > 0) {
			MarketOrder mo = MarketOrder.search(this);
			if (mo != null) {
				Market m = new Market(mo.getKawaipon().getUid());
				m.buy(mo, id);
			}
		}
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StashedCard that = (StashedCard) o;
		return chrome == that.chrome && Objects.equals(card, that.card) && !inCollection;
	}

	@Override
	public int hashCode() {
		return Objects.hash(chrome, card);
	}
}
