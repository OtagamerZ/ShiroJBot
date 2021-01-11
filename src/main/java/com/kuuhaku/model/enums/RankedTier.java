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

import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
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
			String tier = name().toLowerCase().replaceFirst("_.+", "");
			return ImageIO.read(Objects.requireNonNull(TrophyType.class.getClassLoader().getResourceAsStream("shoukan/banners/" + tier + "/" + name().toLowerCase() + ".png")));
		} catch (IOException e) {
			return null;
		}
	}

	public RankedTier getNext() {
		return switch (this) {
			case UNRANKED -> APPRENTICE_IV;
			case APPRENTICE_IV -> APPRENTICE_III;
			case APPRENTICE_III -> APPRENTICE_II;
			case APPRENTICE_II -> APPRENTICE_I;
			case APPRENTICE_I -> INITIATE_IV;
			case INITIATE_IV -> INITIATE_III;
			case INITIATE_III -> INITIATE_II;
			case INITIATE_II -> INITIATE_I;
			case INITIATE_I -> DISCIPLE_IV;
			case DISCIPLE_IV -> DISCIPLE_III;
			case DISCIPLE_III -> DISCIPLE_II;
			case DISCIPLE_II -> DISCIPLE_I;
			case DISCIPLE_I -> ADEPT_IV;
			case ADEPT_IV -> ADEPT_III;
			case ADEPT_III -> ADEPT_II;
			case ADEPT_II -> ADEPT_I;
			case ADEPT_I -> MASTER;
			case MASTER -> ORACLE;
			case ORACLE -> ARCHMAGE;
			case ARCHMAGE -> null;
		};
	}

	public RankedTier getPrevious() {
		return switch (this) {
			case UNRANKED, APPRENTICE_IV -> null;
			case APPRENTICE_III -> APPRENTICE_IV;
			case APPRENTICE_II -> APPRENTICE_III;
			case APPRENTICE_I -> APPRENTICE_II;
			case INITIATE_IV -> APPRENTICE_I;
			case INITIATE_III -> INITIATE_IV;
			case INITIATE_II -> INITIATE_III;
			case INITIATE_I -> INITIATE_II;
			case DISCIPLE_IV -> INITIATE_I;
			case DISCIPLE_III -> DISCIPLE_IV;
			case DISCIPLE_II -> DISCIPLE_III;
			case DISCIPLE_I -> DISCIPLE_II;
			case ADEPT_IV -> DISCIPLE_I;
			case ADEPT_III -> ADEPT_IV;
			case ADEPT_II -> ADEPT_III;
			case ADEPT_I -> ADEPT_II;
			case MASTER -> ADEPT_I;
			case ORACLE -> MASTER;
			case ARCHMAGE -> MASTER;
		};
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
				.replaceFirst("\\s(I|II|III|IV)", "");
	}
}