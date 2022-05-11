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

package com.kuuhaku.model.enums.shoukan;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.utils.IO;

import java.awt.image.BufferedImage;

public enum Race {
	// Pure races
	HUMAN(0x1),
	CREATURE(0x2),
	MACHINE(0x4),
	DIVINITY(0x8),
	SPIRIT(0x10),
	UNDEAD(0x20),
	MYSTICAL(0x40),
	DEMON(0x80),

	// Semi-races
	WEREBEAST(HUMAN.flag | CREATURE.flag),
	CYBORG(HUMAN.flag | MACHINE.flag),
	DEMIGOD(HUMAN.flag | DIVINITY.flag),
	FETCH(HUMAN.flag | SPIRIT.flag),
	LICH(HUMAN.flag | UNDEAD.flag),
	ELF(HUMAN.flag | MYSTICAL.flag),
	ONI(HUMAN.flag | DEMON.flag),
	WARBEAST(CREATURE.flag | MACHINE.flag),
	PRIMAL(CREATURE.flag | DIVINITY.flag),
	FAMILIAR(CREATURE.flag | SPIRIT.flag),
	GHOUL(CREATURE.flag | UNDEAD.flag),
	FEY(CREATURE.flag | MYSTICAL.flag),
	FIEND(CREATURE.flag | DEMON.flag),
	EX_MACHINA(MACHINE.flag | DIVINITY.flag),
	SHIKI(MACHINE.flag | SPIRIT.flag),
	VIRUS(MACHINE.flag | UNDEAD.flag),
	FABLED(MACHINE.flag | MYSTICAL.flag),
	POSSESSED(MACHINE.flag | DEMON.flag),
	HERALD(DIVINITY.flag | SPIRIT.flag),
	SHINIGAMI(DIVINITY.flag | UNDEAD.flag),
	PRIMORDIAL(DIVINITY.flag | MYSTICAL.flag),
	FALLEN(DIVINITY.flag | DEMON.flag),
	GHOST(SPIRIT.flag | UNDEAD.flag),
	PIXIE(SPIRIT.flag | MYSTICAL.flag),
	TORMENTED(SPIRIT.flag | DEMON.flag),
	REBORN(UNDEAD.flag | MYSTICAL.flag),
	SPAWN(UNDEAD.flag | DEMON.flag),
	IMP(MYSTICAL.flag | DEMON.flag),

	NONE(0x0);

	private final int flag;

	Race(int flag) {
		this.flag = flag;
	}

	public String getName(I18N locale) {
		return locale.get("race/" + name());
	}

	public String getMajor(I18N locale) {
		return locale.get("major/" + name());
	}

	public String getMinor(I18N locale) {
		return locale.get("minor/" + name());
	}

	public String getSynergy(I18N locale) {
		return locale.get("synergy/" + name());
	}

	public boolean isRace(Race race) {
		return (this.flag & race.flag) == race.flag;
	}

	public Race fuse(Race with) {
		int res = this.flag | with.flag;
		for (Race r : values()) {
			if (r.flag == res) return r;
		}

		return NONE;
	}

	public BufferedImage getImage() {
		return IO.getResourceAsImage("shoukan/race/full/" + name() + ".png");
	}

	public BufferedImage getIcon() {
		return IO.getResourceAsImage("shoukan/race/icon/" + name() + ".png");
	}
}
