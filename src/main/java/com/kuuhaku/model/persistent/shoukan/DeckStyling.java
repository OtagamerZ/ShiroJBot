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

package com.kuuhaku.model.persistent.shoukan;

import com.kuuhaku.model.enums.DeckOrder;
import com.kuuhaku.model.persistent.shiro.Card;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
public class DeckStyling implements Serializable {
	@Serial
	private static final long serialVersionUID = -3791252523071567880L;

	@Enumerated(EnumType.STRING)
	@Column(name = "senshi_order", nullable = false)
	private DeckOrder senshiOrder = DeckOrder.NAME;

	@Enumerated(EnumType.STRING)
	@Column(name = "evogear_order", nullable = false)
	private DeckOrder evogearOrder = DeckOrder.NAME;

	@ManyToOne
	@JoinColumn(name = "frame_id")
	@Fetch(FetchMode.JOIN)
	private FrameSkin frame;

	@ManyToOne
	@JoinColumn(name = "skin_id")
	@Fetch(FetchMode.JOIN)
	private SlotSkin skin;

	@ManyToOne
	@JoinColumn(name = "cover_id")
	@Fetch(FetchMode.JOIN)
	private Card cover;

	@Column(name = "use_chrome", nullable = false)
	private boolean useChrome;

	public FrameSkin getFrame() {
		return frame;
	}

	public void setFrame(FrameSkin frame) {
		this.frame = frame;
	}

	public SlotSkin getSkin() {
		return skin;
	}

	public void setSkin(SlotSkin slot) {
		this.skin = slot;
	}

	public DeckOrder getSenshiOrder() {
		if (senshiOrder == null || !senshiOrder.isAllowed(Senshi.class)) {
			return DeckOrder.NAME;
		}

		return senshiOrder;
	}

	public void setSenshiOrder(DeckOrder order) {
		this.senshiOrder = order;
	}

	public DeckOrder getEvogearOrder() {
		if (evogearOrder == null || !evogearOrder.isAllowed(Evogear.class)) {
			return DeckOrder.NAME;
		}

		return evogearOrder;
	}

	public void setEvogearOrder(DeckOrder order) {
		this.evogearOrder = order;
	}

	public Card getCover() {
		return cover;
	}

	public void setCover(Card cover) {
		this.cover = cover;
	}

	public boolean isUsingChrome() {
		return useChrome;
	}

	public void setUsingChrome(boolean useChrome) {
		this.useChrome = useChrome;
	}
}
