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

package com.kuuhaku.model.enums.shoukan;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Race {
	// Pure races
	HUMAN(0x1),
	BEAST(0x2),
	MACHINE(0x4),
	DIVINITY(0x8),
	SPIRIT(0x10),
	UNDEAD(0x20),
	MYSTICAL(0x40),
	DEMON(0x80),

	// Semi-races
	WEREBEAST(HUMAN.flag | BEAST.flag),
	CYBORG(HUMAN.flag | MACHINE.flag),
	DEMIGOD(HUMAN.flag | DIVINITY.flag),
	ESPER(HUMAN.flag | SPIRIT.flag),
	LICH(HUMAN.flag | UNDEAD.flag),
	ELF(HUMAN.flag | MYSTICAL.flag),
	ONI(HUMAN.flag | DEMON.flag),
	CYBERBEAST(BEAST.flag | MACHINE.flag),
	PRIMAL(BEAST.flag | DIVINITY.flag),
	FAMILIAR(BEAST.flag | SPIRIT.flag),
	GHOUL(BEAST.flag | UNDEAD.flag),
	FEY(BEAST.flag | MYSTICAL.flag),
	FIEND(BEAST.flag | DEMON.flag),
	EX_MACHINA(MACHINE.flag | DIVINITY.flag),
	SHIKIGAMI(MACHINE.flag | SPIRIT.flag),
	VIRUS(MACHINE.flag | UNDEAD.flag),
	FABLED(MACHINE.flag | MYSTICAL.flag),
	POSSESSED(MACHINE.flag | DEMON.flag),
	HERALD(DIVINITY.flag | SPIRIT.flag),
	SHINIGAMI(DIVINITY.flag | UNDEAD.flag),
	ANGEL(DIVINITY.flag | MYSTICAL.flag),
	FALLEN(DIVINITY.flag | DEMON.flag),
	GHOST(SPIRIT.flag | UNDEAD.flag),
	PIXIE(SPIRIT.flag | MYSTICAL.flag),
	TORMENTED(SPIRIT.flag | DEMON.flag),
	REBORN(UNDEAD.flag | MYSTICAL.flag),
	SPAWN(UNDEAD.flag | DEMON.flag),
	IMP(MYSTICAL.flag | DEMON.flag),

	DOPPELGANGER(HUMAN.flag | BEAST.flag),
	HOMUNCULUS(HUMAN.flag | MACHINE.flag),
	ORACLE(HUMAN.flag | DIVINITY.flag),
	DRYAD(HUMAN.flag | SPIRIT.flag),
	VAMPIRE(HUMAN.flag | UNDEAD.flag),
	DARK_ELF(HUMAN.flag | MYSTICAL.flag),
	CONDEMNED(HUMAN.flag | DEMON.flag),
	GARGOYLE(BEAST.flag | MACHINE.flag),
	CELESTIAL(BEAST.flag | DIVINITY.flag),
	ALIEN(BEAST.flag | SPIRIT.flag),
	SLIME(BEAST.flag | UNDEAD.flag),
	DRAGON(BEAST.flag | MYSTICAL.flag),
	INFERNAL(BEAST.flag | DEMON.flag),
	SENTINEL(MACHINE.flag | DIVINITY.flag),
	GEIST(MACHINE.flag | SPIRIT.flag),
	REVENANT(MACHINE.flag | UNDEAD.flag),
	GOLEM(MACHINE.flag | MYSTICAL.flag),
	DAEMON(MACHINE.flag | DEMON.flag),
	DJINN(DIVINITY.flag | SPIRIT.flag),
	REAPER(DIVINITY.flag | UNDEAD.flag),
	ELEMENTAL(DIVINITY.flag | MYSTICAL.flag),
	ELDRITCH(DIVINITY.flag | DEMON.flag),
	WRAITH(SPIRIT.flag | UNDEAD.flag),
	FAERIE(SPIRIT.flag | MYSTICAL.flag),
	NIGHTMARE(SPIRIT.flag | DEMON.flag),
	MUMMY(UNDEAD.flag | MYSTICAL.flag),
	DULLAHAN(UNDEAD.flag | DEMON.flag),
	SUCCUBUS(MYSTICAL.flag | DEMON.flag),

	MIXED(Integer.MAX_VALUE),
	NONE(0);

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

	public String getDescription(I18N locale) {
		return locale.get("race/" + name() + "_desc");
	}

	public boolean isRace(Race race) {
		return (flag & race.flag) == race.flag;
	}

	public boolean isPure() {
		return Integer.bitCount(flag) < 2;
	}

	public Race fuse(Race with) {
		if (Integer.bitCount(flag | with.flag) > 2) return this;

		return getByFlag(flag | with.flag);
	}

	public Race plus(Race with) {
		if (Integer.bitCount(flag) > 1) return this;

		return fuse(with);
	}

	public Race minus(Race without) {
		if (Integer.bitCount(flag) < 2) return this;

		return Race.getByFlag(flag & ~without.flag);
	}

	public List<Race> split() {
		return split(false);
	}

	public List<Race> split(boolean pure) {
		List<Race> races = new ArrayList<>();

		int bits = flag;
		int i = 1;
		while (bits > 0) {
			if ((bits & 1) == 1) {
				Race r = getByFlag(i);
				if (!pure || r.isPure()) {
					races.add(r);
				}
			}

			i <<= 1;
			bits >>= 1;
		}

		return races;
	}

	public Race[] derivates() {
		List<Race> races = new ArrayList<>();

		for (Race r : validValues()) {
			if (r != this && r.isRace(this)) {
				races.add(r);
			}
		}

		return races.toArray(Race[]::new);
	}

	public BufferedImage getImage() {
		return IO.getResourceAsImage("shoukan/race/full/" + name() + ".png");
	}

	public BufferedImage getIcon() {
		return IO.getResourceAsImage("shoukan/race/icon/" + name() + ".png");
	}

	public BufferedImage getBadge() {
		if (Integer.bitCount(flag) == 0) return null;

		BufferedImage bi = getImage();
		int thickness = bi.getWidth() / 10;

		BufferedImage out = new BufferedImage(bi.getWidth() + thickness * 2, bi.getHeight() + thickness * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		Polygon poly = Graph.makePoly(new Dimension(bi.getWidth() + thickness, bi.getHeight() + thickness),
				0.5, 0,
				1, 1 / 4d,
				1, 1 / 4d * 3,
				0.5, 1,
				0, 1 / 4d * 3,
				0, 1 / 4d
		);
		poly.translate(thickness / 2, thickness / 2);

		g2d.setColor(new Color(50, 50, 50, 127));
		g2d.fill(poly);

		if (Integer.bitCount(flag) == 1) {
			g2d.setColor(Graph.getColor(bi));
		} else {
			List<Race> subs = split();

			g2d.setPaint(new GradientPaint(
					0, 0, subs.getFirst().getColor(),
					bi.getWidth(), 0, subs.get(1).getColor()
			));
		}

		g2d.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.draw(poly);

		g2d.drawImage(bi, out.getWidth() / 2 - bi.getWidth() / 2, out.getHeight() / 2 - bi.getHeight() / 2, null);

		g2d.dispose();

		return out;
	}

	public Color getColor() {
		List<Race> pures = split();
		if (pures.size() == 1) {
			return Graph.getColor(getImage());
		} else {
			return Graph.mix(Graph.getColor(pures.get(0).getImage()), Graph.getColor(pures.get(1).getImage()));
		}
	}

	public int getFlag() {
		return flag;
	}

	public int getCount() {
		return DAO.queryNative(Integer.class, "SELECT count(1) FROM senshi WHERE race = ?1 OR race IN ?2",
				name(), Arrays.stream(derivates()).map(Race::name).toList()
		);
	}

	public Race getVariant() {
		for (Race race : validValues()) {
			if (race != this && race.flag == flag) return race;
		}

		return this;
	}

	public static Race getByFlag(int flag) {
		for (Race race : validValues()) {
			if (race.flag == flag) return race;
		}

		return NONE;
	}

	public static Race[] validValues() {
		return Arrays.stream(values())
				.filter(r -> r.ordinal() < MIXED.ordinal())
				.toArray(Race[]::new);
	}
}
