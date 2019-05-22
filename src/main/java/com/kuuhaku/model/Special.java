/*
 * Copyright (C) 2019 Yago Garcia Sanches Gimenez / KuuHaKu
 *
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see https://www.gnu.org/licenses/
 */

package com.kuuhaku.model;

import java.util.Random;

public class Special {
    private String name, type, description;
    private int diff, move;
    private boolean delayed;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDiff() {
        return diff;
    }

    public void setDiff(int diff) {
        this.diff = diff;
    }

    public int getMove() {
        return move;
    }

    public void setMove(int move) {
        this.move = move;
    }

    public boolean isDelayed() {
        return delayed;
    }

    public void setDelayed() {
        this.delayed = true;
    }
}

class Result extends Special {
    private boolean miss;
    private int damage;
    private float fac;
    private Random chance = new Random();
    public Result calcResult(Special s, Beyblade bb, Beyblade op) {

        if (chance.nextInt(100) > s.getDiff() - bb.getSpeed()) {
            switch (s.getType()) {
                case "VORTEX":

                    break;
                case "TITAN":

                    break;
                case "MOUNTAIN":
                    switch (s.getMove()) {
                        case 1:
                            fac = bb.getStability() / 10.0f;

                            break;
                        case 2:

                            break;
                    }
                    break;
            }
        } else {
            miss = true;
            return this;
        }
        return null;
    }
}

class speedType extends Special {
    public static Special TwinTigerShiningBolt() {
        speedType s = new speedType();
        s.setName("Tigres gêmeos: Relâmpago dourado");
        s.setDescription("Acelera a Beyblade a velocidades extremas, criando uma ilusão dela e avançando em direção ao oponente, causando um alto dano baseado em sua velocidade.");
        s.setType("VORTEX");
        s.setDiff(55);
        s.setMove(1);

        return s;
    }

    public static Special TwinTigerTigerWhip() {
        speedType s = new speedType();
        s.setName("Tigres gêmeos: Açoite do tigre");
        s.setDescription("Move sua Beyblade em direção ao oponente, mas muda de rota repentinamente, fazendo com que uma onda de choque acerte o inimigo, possivelmente atordoando-o.");
        s.setType("VORTEX");
        s.setDiff(70);
        s.setMove(2);

        return s;
    }
}

class powerType extends Special {
    public static Special RunicDragonMeteorImpact() {
        powerType s = new powerType();
        s.setName("Dragão rúnico: Impacto meteoro");
        s.setDescription("Arremessa o oponente para o alto, e utilizando sua própria defesa, impacta-o no chão causando um grande dano.");
        s.setType("TITAN");
        s.setDiff(75);
        s.setMove(1);

        return s;
    }

    public static Special RunicDragonScorchingCharge() {
        powerType s = new powerType();
        s.setName("Dragão rúnico: Avanço escaldante");
        s.setDescription("Concentra o poder do dragão na borda de sua Beyblade, fazendo com que seu próximo ataque cause 3x mais dano.");
        s.setType("TITAN");
        s.setDiff(85);
        s.setMove(2);

        return s;
    }
}

class defenseType extends Special {
    public static Special GuardianBearDisarmingAura() {
        defenseType s = new defenseType();
        s.setName("Urso guardião: Aura desarmante");
        s.setDescription("Gera uma aura em torno de sua Beyblade, fazendo com que o próximo ataque de seu oponente cause metade do dano.");
        s.setType("MOUNTAIN");
        s.setDiff(50);
        s.setMove(1);
        s.setDelayed();

        return s;
    }

    public static Special GuardianBearSpikeArmor() {
        defenseType s = new defenseType();
        s.setName("Urso guardião: Armadura de espinhos");
        s.setDescription("Cria espinhos de aço em sua Beyblade, refletindo uma porcentagem do dano baseada em sua defesa.");
        s.setType("MOUNTAIN");
        s.setDiff(0);
        s.setMove(2);
        s.setDelayed();

        return s;
    }
}
