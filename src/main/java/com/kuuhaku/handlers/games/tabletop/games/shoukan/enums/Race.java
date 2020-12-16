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

import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public enum Race {
	HUMAN("Humano", "Apesar da maioria não possuir afinidade para magia, são numerosos e astutos o suficiente para derrotarem até o maior dos exércitos com sua rápida aprendizagem e vasta tecnologia."),
	ELF("Elfo", "Vivendo em meio a selvas e bosques, possuem a maior afinidade mágica dentre os mortais. Seus aguçados sentidos e agilidade tornam-os altamente mortais no campo de batalha."),
	BESTIAL("Bestial", "Metade humano e metade fera, possuem uma incrível força e instintos aguçados. Não se engane, uma garota-gato ainda é mortal o suficiente para te pegar desprevenido."),
	UNDEAD("Morto-vivo", "Guerreiros mortos a muito tempo e revividos através de magia. São imunes a dor o que os torna implacáveis em combate."),
	MACHINE("Máquina", "Máquinas infundidas com magia, permitindo que ajam por vontade própria e até mesmo tenham emoções. São imbatíveis quando o assunto é poder de fogo."),
	DIVINITY("Divindade", "Divindades que criaram formas físicas para interagir com o mundo dos mortais. Seu poder vem da crença de seus seguidores, o que permite que criem e destruam matéria com um mero estalar de dedos."),
	MYSTICAL("Místico", "Seres místicos resultantes da materialização de energia mágica. Vivem em eterno vínculo com o ambiente e são capazes de sentir até mesmo o menor movimento apenas canalizando seus sentidos."),
	CREATURE("Criatura", "Criaturas sencientes que são capazes de raciocinar e comunicarem-se com os seres ao redor. Apesar disso, sua natureza selvagem ainda os torna perigosos e ferozes caso sejam intimidados."),
	SPIRIT("Espírito", "Almas e espíritos de pessoas e criaturas que não puderam quebrar o vínculo ao mundo material. Algumas tornam-se almas penadas, fazendo-as tornarem-se hostis e malígnas, mas outras conseguem manter sua essência intacta."),
	DEMON("Demônio", "Seres das trevas que vieram ao mundo material para coletar almas para aumentar seu poder. Sua astúcia e metodologia geralmente reflete seu status no submundo, e são altamente temidas por todos os seres vivos.");

	private final String name;
	private final String description;

	Race(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
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
		return switch (this) {
			case HUMAN -> "Humano";
			case ELF -> "Elfo";
			case BESTIAL -> "Bestial";
			case UNDEAD -> "Morto-vivo";
			case MACHINE -> "Máquina";
			case DIVINITY -> "Divindade";
			case MYSTICAL -> "Místico";
			case CREATURE -> "Criatura";
			case SPIRIT -> "Espírito";
			case DEMON -> "Demônio";
		};
	}

	public static Race getByName(String name) {
		return Arrays.stream(values()).filter(c -> Helper.equalsAny(name, StringUtils.stripAccents(c.name), c.name, c.name())).findFirst().orElse(null);
	}
}
