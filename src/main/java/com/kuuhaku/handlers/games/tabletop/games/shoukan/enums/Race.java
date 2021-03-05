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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.enums;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public enum Race {
	HUMAN("Humano",
			"+1500 HP, -1 Mana",
			"+250 HP",
			"Apesar da maioria não possuir afinidade para magia, são numerosos e astutos o suficiente para derrotarem até o maior dos exércitos com sua rápida aprendizagem e vasta tecnologia."
	),
	ELF("Elfo",
			"+500 HP, +1 Mana",
			"+250 HP",
			"Vivendo em meio a selvas e bosques, possuem a maior afinidade mágica dentre os mortais. Seus aguçados sentidos e agilidade torna-os altamente mortais no campo de batalha."
	),
	BESTIAL("Bestial",
			"Inicia com 4 cartas extras",
			"Inicia com 1 ponto de mana extra",
			"Metade humano e metade fera, possuem uma incrível força e instintos aguçados. Não se engane, uma garota-gato ainda é mortal o suficiente para te pegar desprevenido."
	),
	UNDEAD("Morto-vivo",
			"+1% Dano por carta no cemitério",
			"+0,5% Dano por carta no cemitério",
			"Guerreiros mortos a muito tempo e revividos através de magia. São imunes a dor o que os torna implacáveis em combate."
	),
	MACHINE("Máquina",
			"-1 Peso de Equipamentos",
			"Inicia com 1 equipamento extra",
			"Máquinas infundidas com magia, permitindo que ajam por vontade própria e até mesmo tenham emoções. São imbatíveis quando o assunto é poder de fogo."
	),
	DIVINITY("Divindade",
			"-1 Custo de mana (mínimo 1 para campeões)",
			"Inicia com 1 campeão extra",
			"Divindades que criaram formas físicas para interagir com o mundo dos mortais. Seu poder vem da crença de seus seguidores, o que permite que criem e destruam matéria com um mero estalar de dedos."
	),
	MYSTICAL("Místico",
			"-1 Peso de Magias",
			"Inicia com 1 magia extra",
			"Seres místicos resultantes da materialização de energia mágica. Vivem em eterno vínculo com o ambiente e são capazes de sentir até mesmo o menor movimento apenas canalizando seus sentidos."
	),
	CREATURE("Criatura",
			"+2 Limite de cartas na mão",
			"+1 Limite de cartas na mão",
			"Criaturas sencientes que são capazes de raciocinar e comunicarem-se com os seres ao redor. Apesar disso, sua natureza selvagem ainda os torna perigosos e ferozes caso sejam intimidados."
	),
	SPIRIT("Espírito",
			"+2% Defesa por carta no cemitério",
			"+1% Defesa por carta no cemitério",
			"Almas e espíritos de pessoas e criaturas que não puderam quebrar o vínculo ao mundo material. Algumas tornam-se almas penadas, fazendo-as tornarem-se hostis e malígnas, mas outras conseguem manter sua essência intacta."
	),
	DEMON("Demônio",
			"-2000 HP, +2 Mana",
			"-500 HP",
			"Seres das trevas que vieram ao mundo material para coletar almas para aumentar seu poder. Sua astúcia e metodologia geralmente reflete seu status no submundo, e são altamente temidas por todos os seres vivos."
	),
	NONE("Nenhum",
			"Nenhum",
			"Nenhum",
			"Nenhum"
	);

	private final String name;
	private final String majorDesc;
	private final String minorDesc;
	private final String description;

	Race(String name, String majorDesc, String minorDesc, String description) {
		this.name = name;
		this.majorDesc = majorDesc;
		this.minorDesc = minorDesc;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getMajorDesc() {
		return majorDesc;
	}

	public String getMinorDesc() {
		return minorDesc;
	}

	public String getDescription() {
		return description;
	}

	public BufferedImage getIcon() {
		try {
			return ImageIO.read(Objects.requireNonNull(FrameColor.class.getClassLoader().getResourceAsStream("shoukan/race/" + name().toLowerCase() + ".png")));
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return name;
	}

	public static Race getByName(String name) {
		return Arrays.stream(values()).filter(c -> Helper.equalsAny(name, StringUtils.stripAccents(c.name), c.name, c.name())).findFirst().orElse(null);
	}

	public static Pair<Race, Race> getCombo(List<Champion> champs) {
		List<Race> races = champs.stream()
				.map(Champion::getRace)
				.collect(Collectors.toList());
		Map<Race, Integer> counts = new HashMap<>();

		for (Race race : Race.validValues()) {
			counts.put(race, Collections.frequency(races, race));
		}

		Race major = counts.entrySet().stream()
				.max(Comparator
						.<Map.Entry<Race, Integer>>comparingInt(Map.Entry::getValue)
						.thenComparing(e -> e.getKey().getName(), String.CASE_INSENSITIVE_ORDER)
				)
				.map(Map.Entry::getKey)
				.orElse(NONE);

		counts.remove(major);

		Race minor = counts.entrySet().stream()
				.max(Comparator
						.<Map.Entry<Race, Integer>>comparingInt(Map.Entry::getValue)
						.thenComparing(e -> e.getKey().getName(), String.CASE_INSENSITIVE_ORDER)
				)
				.map(Map.Entry::getKey)
				.orElse(NONE);

		return Pair.of(major, minor);
	}

	public static Race[] validValues() {
		return Arrays.stream(values()).filter(r -> r != NONE).toArray(Race[]::new);
	}
}
