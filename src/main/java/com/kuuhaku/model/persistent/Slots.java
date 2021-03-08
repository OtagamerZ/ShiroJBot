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

package com.kuuhaku.model.persistent;

import com.kuuhaku.controller.postgresql.SlotsDAO;
import com.kuuhaku.model.common.GamblePool;
import com.kuuhaku.utils.Helper;

import javax.persistence.*;

@Entity
@Table(name = "slots")
public class Slots {
	public static final String SLOT = "<a:slots:680448443692744956>";
	public static final String JACKPOT = "<:7_s:680405919057969155>";
	public static final String DIAMOND = "<:diamond_s:680405919317753877>";
	public static final String HORSESHOE = "<:horseshoe_s:680405919213158525>";
	public static final String BAR = "<:bar_s:680405918667898910>";
	public static final String BELL = "<:bell_s:680405919732990017>";
	public static final String HEART = "<:heart_s:680405919183405086>";
	public static final String CHERRY = "<:cherry_s:680448442832912419>";
	public static final String WATERMELON = "<:watermelon_s:680405919548440587>";
	public static final String LEMON = "<:lemon_s:680405919901024284>";
	private static final String[] slots;

	static {
		GamblePool gp = new GamblePool();
		String[] icon = {LEMON, WATERMELON, CHERRY, HEART, BELL, BAR, HORSESHOE, DIAMOND, JACKPOT};
		for (String s : icon) {
			gp.addGamble(new GamblePool.Gamble(s, s.equals(JACKPOT) ? 2 : 5));
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

	public static String getSlot() {
		return slots[Helper.rng(slots.length, true)];
	}

	public static String[] getSlots() {
		return slots;
	}
}
