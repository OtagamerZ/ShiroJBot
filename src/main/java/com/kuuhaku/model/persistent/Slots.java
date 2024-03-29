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

import com.kuuhaku.controller.postgresql.SlotsDAO;
import com.kuuhaku.model.common.GamblePool;
import com.kuuhaku.model.enums.Slot;
import com.kuuhaku.utils.Helper;

import javax.persistence.*;

import static com.kuuhaku.model.enums.Slot.*;

@Entity
@Table(name = "slots")
public class Slots {
	public static final String SLOT = "<a:slots:680448443692744956>";
	private static final Slot[] slots;

	static {
		GamblePool gp = new GamblePool();
		Slot[] icon = {LEMON, WATERMELON, CHERRY, HEART, BELL, BAR, HORSESHOE, DIAMOND, JACKPOT};
		for (Slot s : icon) {
			gp.addGamble(new GamblePool.Gamble(s, s.ordinal() + 1));
		}
		slots = gp.getPool();
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long pot;

	public long getPot() {
		return pot;
	}

	public long jackpot() {
		long prize = pot;
		this.pot = 0;
		SlotsDAO.saveSlots(this);
		return prize;
	}

	public void addToPot(long value) {
		this.pot += value;
	}

	public static Slot getSlot() {
		return Helper.getRandomEntry(slots);
	}

	public static Slot[] getSlots() {
		return slots;
	}
}
