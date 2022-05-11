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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Drawable;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.utils.Bit;
import com.kuuhaku.utils.Graph;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Table(name = "senshi")
public class Senshi extends DAO implements Drawable {
	@Id
	@Column(name = "card_id", nullable = false)
	private String id;

	@OneToOne(optional = false, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private Card card;

	@Enumerated(EnumType.STRING)
	@Column(name = "race", nullable = false)
	private Race race;

	@Embedded
	private CardAttributes base;

	private transient Pair<Integer, BufferedImage> cache = null;
	private transient boolean solid = false;
	private transient int state = 0x0;
	/*
	0x00 00 FFF F
	        │││ └ 0011
	        │││     │└ defending
	        │││     └─ flipped
	        ││└─ (0 - 15) stunned
	        │└── (0 - 15) sleeping
	        └─── (0 - 15) stasis
	 */

	public String getId() {
		return id;
	}

	public Card getCard() {
		return card;
	}

	public Race getRace() {
		return race;
	}

	public CardAttributes getBase() {
		return base;
	}

	public List<String> getTags() {
		List<String> out = new ArrayList<>();
		out.add("race/" + race.name());
		for (Object tag : base.getTags()) {
			out.add("tag/" + tag);
		}
		
		return out;
	}

	@Override
	public int getIndex() {
		return 0;
	}

	@Override
	public AtomicInteger getIndexRef() {
		return null;
	}

	@Override
	public Side getSide() {
		return null;
	}

	@Override
	public int getMPCost() {
		return base.getMana();
	}

	@Override
	public int getHPCost() {
		return 5;
	}

	@Override
	public int getDmg() {
		return base.getAtk();
	}

	@Override
	public int getDef() {
		return base.getDef();
	}

	@Override
	public int getDodge() {
		return base.getDodge();
	}

	@Override
	public int getBlock() {
		return base.getBlock();
	}

	@Override
	public boolean isSolid() {
		return solid;
	}

	@Override
	public void setSolid(boolean solid) {
		this.solid = solid;
	}

	public boolean isDefending() {
		return Bit.on(state, 0);
	}

	public void setDefending(boolean defending) {
		state = Bit.set(state, 0, defending);
	}

	@Override
	public boolean isFlipped() {
		return Bit.on(state, 1);
	}

	@Override
	public void setFlipped(boolean flipped) {
		state = Bit.set(state, 1, flipped);
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		int hash = renderHashCode(locale);
		if (cache == null || cache.getFirst() != hash) {
			if (isFlipped()) return deck.getFrame().getBack(deck);

			String desc = base.getDescription(locale);

			BufferedImage img = card.drawCardNoBorder(deck.isUsingFoil());
			BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = out.createGraphics();
			g2d.setRenderingHints(Constants.HD_HINTS);

			g2d.setClip(deck.getFrame().getBoundary());
			g2d.drawImage(img, 0, 0, null);
			g2d.setClip(null);

			g2d.drawImage(deck.getFrame().getFront(!desc.isEmpty()), 0, 0, null);
			g2d.drawImage(race.getIcon(), 10, 12, null);

			g2d.setFont(new Font("Arial", Font.BOLD, 20));
			g2d.setColor(deck.getFrame().getPrimaryColor());
			Graph.drawOutlinedString(g2d, StringUtils.abbreviate(card.getName(), Drawable.MAX_NAME_LENGTH), 38, 30, 2, deck.getFrame().getBackgroundColor());

			g2d.setColor(deck.getFrame().getSecondaryColor());
			g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 12));
			g2d.drawString(getTags().stream().map(locale::get).toList().toString(), 7, 275);

			g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 10));
			Graph.drawMultilineString(g2d, StringUtils.abbreviate(desc, Drawable.MAX_DESC_LENGTH), 7, 285, 211);

			drawCosts(g2d);
			drawAttributes(g2d);

			g2d.dispose();

			cache = new Pair<>(hash, out);
		}

		return cache.getSecond();
	}

	@Override
	public int renderHashCode(I18N locale) {
		return Objects.hash(card, race, base, state, locale);
	}
}
