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
import java.util.Objects;

public enum RankedTier {
	UNRANKED(0, "Sem ranking", 10),

	INITIATE_IV(1, "Iniciado IV", 3),
	INITIATE_III(1, "Iniciado III", 3),
	INITIATE_II(1, "Iniciado II", 3),
	INITIATE_I(1, "Iniciado I", 5),

	APPRENTICE_IV(2, "Aprendiz IV", 3),
	APPRENTICE_III(2, "Aprendiz III", 3),
	APPRENTICE_II(2, "Aprendiz II", 3),
	APPRENTICE_I(2, "Aprendiz I", 5),

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
		return Helper.getNext(this,
				UNRANKED,
				INITIATE_IV,
				INITIATE_III,
				INITIATE_II,
				INITIATE_I,
				APPRENTICE_IV,
				APPRENTICE_III,
				APPRENTICE_II,
				APPRENTICE_I,
				DISCIPLE_IV,
				DISCIPLE_III,
				DISCIPLE_II,
				DISCIPLE_I,
				ADEPT_IV,
				ADEPT_III,
				ADEPT_II,
				ADEPT_I,
				MASTER,
				ORACLE,
				ARCHMAGE
		);
	}

	public RankedTier getPrevious() {
		return Helper.getPrevious(this,
				null,
				INITIATE_IV,
				INITIATE_III,
				INITIATE_II,
				INITIATE_I,
				APPRENTICE_IV,
				APPRENTICE_III,
				APPRENTICE_II,
				APPRENTICE_I,
				DISCIPLE_IV,
				DISCIPLE_III,
				DISCIPLE_II,
				DISCIPLE_I,
				ADEPT_IV,
				ADEPT_III,
				ADEPT_II,
				ADEPT_I,
				MASTER,
				ORACLE,
				ARCHMAGE
		);
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