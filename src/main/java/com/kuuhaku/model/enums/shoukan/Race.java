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
	CONDEMNED(HUMAN.flag | DEMON.flag),
	GARGOYLE(BEAST.flag | MACHINE.flag),
	CELESTIAL(BEAST.flag | DIVINITY.flag),
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
	ELEMENTAL(DIVINITY.flag | MYSTICAL.flag),
	FALLEN(DIVINITY.flag | DEMON.flag),
	GHOST(SPIRIT.flag | UNDEAD.flag),
	PIXIE(SPIRIT.flag | MYSTICAL.flag),
	TORMENTED(SPIRIT.flag | DEMON.flag),
	REBORN(UNDEAD.flag | MYSTICAL.flag),
	SPAWN(UNDEAD.flag | DEMON.flag),
	SUCCUBUS(MYSTICAL.flag | DEMON.flag),

	DOPPELGANGER(HUMAN.flag | BEAST.flag, true),
	HOMUNCULUS(HUMAN.flag | MACHINE.flag, true),
	ORACLE(HUMAN.flag | DIVINITY.flag, true),
	DRYAD(HUMAN.flag | SPIRIT.flag, true),
	VAMPIRE(HUMAN.flag | UNDEAD.flag, true),
	DARK_ELF(HUMAN.flag | MYSTICAL.flag, true),
	ONI(HUMAN.flag | DEMON.flag, true),
	CYBERBEAST(BEAST.flag | MACHINE.flag, true),
	PRIMAL(BEAST.flag | DIVINITY.flag, true),
	ALIEN(BEAST.flag | SPIRIT.flag, true),
	SLIME(BEAST.flag | UNDEAD.flag, true),
	DRAGON(BEAST.flag | MYSTICAL.flag, true),
	INFERNAL(BEAST.flag | DEMON.flag, true),
	SENTINEL(MACHINE.flag | DIVINITY.flag, true),
	GEIST(MACHINE.flag | SPIRIT.flag, true),
	REVENANT(MACHINE.flag | UNDEAD.flag, true),
	GOLEM(MACHINE.flag | MYSTICAL.flag, true),
	DAEMON(MACHINE.flag | DEMON.flag, true),
	DJINN(DIVINITY.flag | SPIRIT.flag, true),
	REAPER(DIVINITY.flag | UNDEAD.flag, true),
	ANGEL(DIVINITY.flag | MYSTICAL.flag, true),
	ELDRITCH(DIVINITY.flag | DEMON.flag, true),
	WRAITH(SPIRIT.flag | UNDEAD.flag, true),
	FAERIE(SPIRIT.flag | MYSTICAL.flag, true),
	NIGHTMARE(SPIRIT.flag | DEMON.flag, true),
	MUMMY(UNDEAD.flag | MYSTICAL.flag, true),
	DULLAHAN(UNDEAD.flag | DEMON.flag, true),
	IMP(MYSTICAL.flag | DEMON.flag, true),

	MIXED(Integer.MAX_VALUE),
	NONE(0);

	private final int flag;
	private final boolean variant;

	Race(int flag) {
		this(flag, false);
	}

	Race(int flag, boolean variant) {
		this.flag = flag;
		this.variant = variant;
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
		List<Race> races = new ArrayList<>();
		if (isPure()) {
			races.add(this);
			return races;
		}

		int bits = flag;
		int i = 1;
		while (bits > 0) {
			if ((bits & 1) == 1) {
				Race r = getByFlag(i);
				if (r != NONE) {
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
			float[] stops = new float[subs.size()];
			Color[] colors = new Color[subs.size()];

			for (int i = 0; i < subs.size(); i++) {
				stops[i] = 1f / (subs.size() - 1) * i;
				colors[i] = subs.get(i).getColor();
			}

			g2d.setPaint(new LinearGradientPaint(
					0, 0, out.getWidth(), 0,
					stops, colors
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

	public boolean isVariant() {
		return variant;
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

	public static Race[] pureValues() {
		return Arrays.stream(values())
				.filter(Race::isPure)
				.toArray(Race[]::new);
	}

	public static Race[] validValues() {
		return Arrays.stream(values())
				.filter(r -> r.ordinal() < MIXED.ordinal())
				.toArray(Race[]::new);
	}
}
