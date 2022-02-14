/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

public enum Race {
    HUMAN("Humano",
            "**(SOLO)** +1 limite de cópias para campeões **(DUO)** Você e seu parceiro dividem a mesma mão (+3 limite de cartas na mão)",
            "Campeões com 1 ou 2 de custo no deck reduzem o dano sofrido em (**SOLO** | 1%/**DUO** | 0,5%), 3/4 disso adicionado à esquiva de suas cartas",
            "Apesar da maioria não possuir afinidade para magia, são numerosos e astutos o suficiente para derrotarem até o maior dos exércitos com sua rápida aprendizagem e vasta tecnologia.",
            new Integer[]{2, 2, 2, 2, 2}
    ),
    ELF("Elfo",
            "+1 mana, campos possuem efeitos negativos reduzidos em 50% e positivos aumentados em 25% nas suas cartas",
            "+1 mana a cada 3 turnos",
            "Vivendo em meio a selvas e bosques, possuem a maior afinidade mágica dentre os mortais. Seus aguçados sentidos e agilidade torna-os altamente mortais no campo de batalha.",
            new Integer[]{1, 2, 3, 3, 1}
    ),
    BESTIAL("Bestial",
            "**(SOLO)** +1 limite de cópias para evogears **(DUO)** Cartas descartadas voltam para o deck e embaralham-no",
            "Inicia com 1 ponto de mana extra",
            "Metade humano e metade fera, possuem uma incrível força e instintos aguçados. Não se engane, uma garota-gato ainda é mortal o suficiente para te pegar desprevenido.",
            new Integer[]{3, 2, 3, 1, 1}
    ),
    MACHINE("Máquina",
            "**(SOLO)** -1 peso de equipamentos **(DUO)** Ao puxar um equipamento ganhe 250 de HP",
            "Inicia com 1 equipamento extra",
            "Máquinas infundidas com magia, permitindo que ajam por vontade própria e até mesmo tenham emoções. São imbatíveis quando o assunto é poder de fogo.",
            new Integer[]{1, 3, 1, 0, 4}
    ),
    DIVINITY("Divindade",
            "Cartas sem efeito em seu deck ganham um aleatório de mesmo custo (exceto fusões)",
            "Inicia com 1 campeão extra",
            "Divindades que criaram formas físicas para interagir com o mundo dos mortais. Seu poder vem da crença de seus seguidores, o que permite que criem e destruam matéria com um mero estalar de dedos.",
            new Integer[]{3, 3, 0, 3, 1}
    ),
    MYSTICAL("Místico",
            "**(SOLO)** -1 peso de magias **(DUO)** Ao puxar uma magia ganhe 1 de mana",
            "Inicia com 1 magia extra",
            "Seres místicos resultantes da materialização de energia mágica. Vivem em eterno vínculo com o ambiente e são capazes de sentir até mesmo o menor movimento apenas canalizando seus sentidos.",
            new Integer[]{2, 1, 2, 4, 1}
    ),
    CREATURE("Criatura",
            "+2 limite de cartas na mão",
            "+1 limite de cartas na mão",
            "Criaturas sencientes que são capazes de raciocinar e comunicarem-se com os seres ao redor. Apesar disso, sua natureza selvagem ainda os torna perigosos e ferozes caso sejam intimidados.",
            new Integer[]{3, 3, 3, 1, 0}
    ),
    SPIRIT("Espírito",
            "Adiciona a habilidade de consumir as últimas 3 cartas do cemitério para sintetizar um evogear aleatório (tempo de recarga: 5 turnos)",
            "+1% defesa por carta no cemitério",
            "Almas e espíritos de pessoas e criaturas que não puderam quebrar o vínculo ao mundo material. Algumas tornam-se almas penadas, fazendo-as tornarem-se hostis e malígnas, mas outras conseguem manter sua essência intacta.",
            new Integer[]{2, 4, 2, 2, 0}
    ),
    DEMON("Demônio",
            "-1500 HP, +1 mana para cada 20% de HP perdido do oponente, cura 10% do HP perdido por turno enquanto abaixo de 33%",
            "+25% dano direto causado, +33% dano direto sofrido",
            "Seres das trevas que vieram ao mundo material para coletar almas para aumentar seu poder. Sua astúcia e metodologia geralmente reflete seu status no submundo, e são altamente temidas por todos os seres vivos.",
            new Integer[]{4, 2, 2, 1, 1}
    ),
    UNDEAD("Morto-vivo",
            "Sobrevive 1 turno extra ao atingir 0 de HP (tempo de recarga: 5 turnos)",
            "+1% dano por carta no cemitério",
            "Guerreiros mortos a muito tempo e revividos através de magia. São imunes a dor o que os torna implacáveis em combate.",
            new Integer[]{3, 3, 1, 0, 3}
    ),
    NONE("Nenhum",
            "Nenhum",
            "Nenhum",
            "Nenhum",
            new Integer[0]
    );

    private final String name;
    private final String majorDesc;
    private final String minorDesc;
    private final String description;
    private final Integer[] startingStats;

    Race(String name, String majorDesc, String minorDesc, String description, Integer[] startingStats) {
        this.name = name;
        this.majorDesc = majorDesc;
        this.minorDesc = minorDesc;
        this.description = description;
        this.startingStats = startingStats;
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

    public Integer[] getStartingStats() {
        return startingStats;
    }

    public BufferedImage getIcon() {
        return Helper.getResourceAsImage(this.getClass(), "shoukan/race/" + name().toLowerCase(Locale.ROOT) + ".png");
    }

    public static Race getByName(String name) {
        return Arrays.stream(values()).filter(c -> Helper.equalsAny(name, StringUtils.stripAccents(c.name), c.name, c.name())).findFirst().orElse(null);
    }

    public static Pair<Race, Race> getCombo(List<Champion> champs) {
        if (champs.isEmpty()) return Pair.of(NONE, NONE);

        List<Race> order = champs.stream()
                .map(Champion::getRace)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        Bag<Race> races = champs.stream()
                .map(Champion::getRace)
                .collect(Collectors.toCollection(HashBag::new));

        Race major = NONE;
        for (Race race : races) {
            if (major == NONE) {
                major = race;
                continue;
            }

            int count = races.getCount(race);
            if (count > races.getCount(major)) {
                major = race;
            } else if (count == races.getCount(major) && order.indexOf(race) < order.indexOf(major)) {
                major = race;
            }
        }

        Race minor = NONE;
        if (order.size() > 1) {
            for (Race race : races) {
                if (race == major) continue;

                if (minor == NONE) {
                    minor = race;
                    continue;
                }

                int count = races.getCount(race);
                if (count > races.getCount(minor)) {
                    minor = race;
                } else if (count == races.getCount(minor) && order.indexOf(race) < order.indexOf(minor)) {
                    minor = race;
                }
            }
        }

        return Pair.of(major, minor);
    }

    public static Race[] validValues() {
        return Arrays.stream(values()).filter(r -> r != NONE).toArray(Race[]::new);
    }

    @Override
    public String toString() {
        return name;
    }
}
