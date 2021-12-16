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

public enum BuffType {
    XP(5000, 0.75, 0.3),
    CARD(4000, 0.8, 0.275),
    DROP(3250, 0.65, 0.275),
    FOIL(7500, 0.9, 0.325);

    private final int basePrice;
    private final double priceMult;
    private final double powerMult;

    BuffType(int basePrice, double priceMult, double powerMult) {
        this.basePrice = basePrice;
        this.priceMult = priceMult;
        this.powerMult = powerMult;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public double getPriceMult() {
        return priceMult;
    }

    public double getPowerMult() {
        return powerMult;
    }

    @Override
    public String toString() {
        return "Melhoria de " + switch (this) {
            case XP -> "XP";
            case CARD -> "cartas";
            case DROP -> "drops";
            case FOIL -> "cromadas";
        };
    }
}
