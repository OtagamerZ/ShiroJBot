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

package com.kuuhaku.model.enums;

import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public enum Charm {
    SHIELD("Escudo", "Bloqueia %s efeito%s de destruição ou conversão"),
    MIRROR("Reflexo", "Reflete efeitos de destruição ou conversão"),
    TIMEWARP("Salto temporal", "Ativa %s efeito%s por turno instantaneamente"),
    DOUBLETAP("Toque duplo", "Ativa novamente %s efeito%s de invocação"),
    CLONE("Clone", "Cria um clone com %s%% dos atributos"),
    LINK("Vínculo", "Bloqueia modificadores de campo"),
    SPELL("Magia", "Executa um efeito ao ativar"),
    ENCHANTMENT("Encantamento", "Prende-se à uma carta, adicionando um efeito extra à ela"),
	CURSE("Maldição", "Adiciona um efeito negativo ao alvo"),
    TRAP("Armadilha", "Prende-se à uma carta mas virada para baixo, adicionando um efeito de uso único à ela"),
    PIERCING("Penetração", "Causa %s%% do dano final da carta como dano direto"),
    AGILITY("Agilidade", "Aumenta a chance de esquiva em %s%%"),
    DRAIN("Dreno", "Rouba %s de mana ao atacar"),
    BLEEDING("Sangramento", "Reduz o poder de cura em 50%% e adiciona %s%% do dano final da carta como sangramento"),
    FORTIFY("Fortificar", "Aumenta a chance de bloqueio em %s%%");

    private final String name;
    private final String description;

    Charm(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription(int tier) {
        return switch (this) {
            case SHIELD, TIMEWARP, DOUBLETAP -> description.formatted(
                    Calc.getFibonacci(tier),
					Calc.getFibonacci(tier) == 1 ? "" : "s"
            );
			case CLONE -> description.formatted(25 * tier);
            case DRAIN -> description.formatted(Calc.getFibonacci(tier));
            case AGILITY -> description.formatted(Utils.roundToString(7.5 * tier, 1));
            case FORTIFY -> description.formatted(5 * tier);
			case PIERCING, BLEEDING -> description.formatted(4 * tier);
			default -> description;
        };
    }

    public BufferedImage getIcon() {
        if (Utils.equalsAny(this, SPELL, ENCHANTMENT, TRAP)) return null;
        return IO.getResourceAsImage("shoukan/charm/" + name().toLowerCase() + ".png");
    }

    public static BufferedImage getIcon(List<Charm> charms) {
		List<BufferedImage> icons = charms.stream()
				.map(Charm::getIcon)
				.filter(Objects::nonNull)
				.limit(2)
				.toList();

		if (icons.isEmpty()) return null;
		else if (icons.size() == 1) return icons.get(0);

		BufferedImage mask = IO.getResourceAsImage("shoukan/charm/mask.png");
        assert mask != null;

        BufferedImage bi = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		for (int i = 0; i < icons.size(); i++) {
			BufferedImage icon = icons.get(i);
			Graph.applyMask(icon, mask, i, true);
			g2d.drawImage(icon, 0, 0, null);
		}
		g2d.drawImage(IO.getResourceAsImage("shoukan/charm/div.png"), 0, 0, null);
		g2d.dispose();

        return bi;
    }
}
