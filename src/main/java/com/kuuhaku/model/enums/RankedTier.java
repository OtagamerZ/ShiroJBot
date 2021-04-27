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

package com.kuuhaku.model.enums;

import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public enum RankedTier {
	UNRANKED(0, "Sem ranking", 10),

	APPRENTICE_IV(1, "Aprendiz IV", 3),
	APPRENTICE_III(1, "Aprendiz III", 3),
	APPRENTICE_II(1, "Aprendiz II", 3),
	APPRENTICE_I(1, "Aprendiz I", 5),

	INITIATE_IV(2, "Iniciado IV", 3),
	INITIATE_III(2, "Iniciado III", 3),
	INITIATE_II(2, "Iniciado II", 3),
	INITIATE_I(2, "Iniciado I", 5),

	DISCIPLE_IV(3, "Discípulo IV", 3),
	DISCIPLE_III(3, "Discípulo III", 3),
	DISCIPLE_II(3, "Discípulo II", 3),
	DISCIPLE_I(3, "Discípulo I", 5),

	ADEPT_IV(4, "Adepto IV", 3),
	ADEPT_III(4, "Adepto III", 3),
	ADEPT_II(4, "Adepto II", 3),
	ADEPT_I(4, "Adepto I", 5),

	MASTER(5, "Mestre", 5),
	ORACLE(6, "Oráculo", 5),
	ARCHMAGE(7, "Arquimago", 0);

	private final int tier;
	private final String name;
	private final int md;

	RankedTier(int tier, String name, int md) {
		this.tier = tier;
		this.name = name;
		this.md = md;
	}

	public int getTier() {
		return tier;
	}

	public String getName() {
		return name;
	}

	public int getMd() {
		return md;
	}

	public BufferedImage getBanner() {
		if (this == UNRANKED) return null;
		try {
			String tier = name().toLowerCase(Locale.ROOT).replaceFirst("_.+", "");
			return ImageIO.read(Objects.requireNonNull(TrophyType.class.getClassLoader().getResourceAsStream("shoukan/banners/" + tier + "/" + name().toLowerCase(Locale.ROOT) + ".png")));
		} catch (IOException e) {
			return null;
		}
	}

	public int getPromRP() {
		return tier >= RankedTier.MASTER.getTier() ? (tier - 4) * 125 : 100;
	}

	public RankedTier getNext() {
		return Helper.getNext(this, values());
	}

	public RankedTier getPrevious() {
		return Helper.getPrevious(this, values());
	}

	public static String getTierName(int tier, boolean enumName) {
		if (enumName) return Arrays.stream(values())
				.filter(rt -> rt.getTier() == tier)
				.map(RankedTier::name)
				.findFirst()
				.orElseThrow()
				.replaceFirst("_.+", "");
		else return Arrays.stream(values())
				.filter(rt -> rt.getTier() == tier)
				.map(rt -> StringUtils.capitalize(rt.getName()))
				.findFirst()
				.orElseThrow()
				.replaceFirst("\\s(IV|III|II|I)", "");
	}
}