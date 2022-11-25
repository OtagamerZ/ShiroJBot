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

import com.kuuhaku.model.enums.DeckOrder;
import com.kuuhaku.model.enums.shoukan.FrameSkin;
import com.kuuhaku.model.enums.shoukan.SlotSkin;
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
	@Column(name = "frame", nullable = false)
	private FrameSkin frame = FrameSkin.PINK;

	@Enumerated(EnumType.STRING)
	@Column(name = "slot", nullable = false)
	private SlotSkin slot = SlotSkin.DEFAULT;

	@Enumerated(EnumType.STRING)
	@Column(name = "senshi_order", nullable = true)
	private DeckOrder senshiOrder = DeckOrder.NAME;

	@Enumerated(EnumType.STRING)
	@Column(name = "evogear_order", nullable = true)
	private DeckOrder evogearOrder = DeckOrder.NAME;

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

	public SlotSkin getSlot() {
		return slot;
	}

	public void setSlot(SlotSkin slot) {
		this.slot = slot;
	}

	public DeckOrder getSenshiOrder() {
		return senshiOrder;
	}

	public void setSenshiOrder(DeckOrder order) {
		this.senshiOrder = order;
	}

	public DeckOrder getEvogearOrder() {
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
