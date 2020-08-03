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

import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.model.persistent.Blacklist;
import com.kuuhaku.model.persistent.CustomAnswers;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.utils.Helper;

import java.util.ArrayList;
import java.util.List;

public class DataDump {
	private final List<CustomAnswers> caDump;
	private final List<Member> mDump;
	private final List<GuildConfig> gcDump;
	private final List<Kawaigotchi> kgDump;
	private final List<PoliticalState> psDump;
	private final List<Blacklist> blDump;

	public DataDump(List<CustomAnswers> caDump, List<Member> mDump, List<GuildConfig> gcDump, List<Kawaigotchi> kgDump, List<PoliticalState> psDump, List<Blacklist> blDump) {
		this.caDump = caDump;
		this.gcDump = gcDump;
		this.mDump = mDump;
		this.kgDump = kgDump;
		this.psDump = psDump;
		this.blDump = Helper.getOr(blDump, new ArrayList<>());
	}

    public List<CustomAnswers> getCaDump() {
        return caDump;
    }

    public List<Member> getmDump() {
		return mDump;
	}

	public List<GuildConfig> getGcDump() {
		return gcDump;
	}

	public List<Kawaigotchi> getKgDump() {
		return kgDump;
	}

	public List<PoliticalState> getPsDump() {
		return psDump;
	}

	public List<Blacklist> getBlDump() {
		return blDump;
	}
}
