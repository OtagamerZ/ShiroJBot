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

import com.kuuhaku.utils.Helper;

import java.awt.image.BufferedImage;
import java.util.Locale;

public enum Charm {
    SPELLSHIELD("Escudo de feitiços", "Bloqueia efeitos de destruição ou conversão"),
    SPELLMIRROR("Reflexo de feitiços", "Reflete efeitos de destruição ou conversão"),
    TIMEWARP("Salto temporal", "Ativa efeitos por turno instantâneamente"),
    DOUBLETAP("Toque duplo", "Ativa novamente efeitos de invocação."),
    DOPPELGANGER("Clone", "Cria um clone com 75% dos atributos"),
    SOULLINK("Vínculo espiritual", "Bloqueia modificadores de campo"),
    SPELL("Magia", "Não pode ser equipado, mas pode ser ativado"),
    ARMORPIERCING("Penetração de armadura", "Causa dano direto ao atacar"),
    AGILITY("Agilidade", "Aumenta a chance de esquiva em 10%"),
    DRAIN("Dreno", "Rouba 1 de mana ao atacar"),
    BLEEDING("Sangramento", "Reduz curas em 50% e causa dano direto ao longo de 10 turnos ao atacar");

    private final String name;
    private final String description;

    Charm(String name, String description) {
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
        if (this == SPELL) return null;
        return Helper.getResourceAsImage(this.getClass(), "shoukan/charm/" + name().toLowerCase(Locale.ROOT) + ".png");
    }
}
