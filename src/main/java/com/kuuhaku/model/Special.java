/*
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
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model;

public class Special {
    private String name, type, description;
    private int diff;
    private boolean bear = false;

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    public int getDiff() {
        return diff;
    }

    void setDiff(int diff) {
        this.diff = diff;
    }

    static Special getSpecial(int s) {
        switch (s) {
            case 11: return SpeedType.TwinTigerShiningBolt();
            case 12: return SpeedType.TwinTigerTigerWhip();
            case 21: return PowerType.RunicDragonMeteorImpact();
            case 22: return PowerType.RunicDragonScorchingCharge();
            case 31: return DefenseType.GuardianBearDisarmingAura();
            default: return null;
        }
    }

    public boolean isBear() {
        return bear;
    }

    public void setBear(boolean bear) {
        this.bear = bear;
    }
}

class SpeedType extends Special {
    static Special TwinTigerShiningBolt() {
        SpeedType s = new SpeedType();
        s.setName("Tigres gêmeos: Relâmpago dourado");
        s.setDescription("Acelera a Beyblade a velocidades extremas, criando uma ilusão dela e avançando em direção ao oponente, causando um alto dano baseado em sua velocidade.");
        s.setType("TIGER");
        s.setDiff(55);

        return s;
    }

    static Special TwinTigerTigerWhip() {
        SpeedType s = new SpeedType();
        s.setName("Tigres gêmeos: Açoite do tigre");
        s.setDescription("Move sua Beyblade em direção ao oponente, mas muda de rota repentinamente, fazendo com que uma onda de choque acerte o inimigo, atordoando-o.");
        s.setType("TIGER");
        s.setDiff(70);

        return s;
    }
}

class PowerType extends Special {
    static Special RunicDragonMeteorImpact() {
        PowerType s = new PowerType();
        s.setName("Dragão rúnico: Impacto meteoro");
        s.setDescription("Arremessa o oponente para o alto, e utilizando sua própria defesa, impacta-o no chão causando um grande dano.");
        s.setType("DRAGON");
        s.setDiff(75);

        return s;
    }

    static Special RunicDragonScorchingCharge() {
        PowerType s = new PowerType();
        s.setName("Dragão rúnico: Avanço escaldante");
        s.setDescription("Concentra o poder do dragão na borda de sua Beyblade, fazendo com que seu próximo ataque cause 3x mais dano.");
        s.setType("DRAGON");
        s.setDiff(85);

        return s;
    }
}

class DefenseType extends Special {
    static Special GuardianBearDisarmingAura() {
        DefenseType s = new DefenseType();
        s.setName("Urso guardião: Aura desarmante");
        s.setDescription("Gera uma aura em torno de sua Beyblade, fazendo com que o próximo ataque de seu oponente cause metade do dano, sendo também influenciado por sua estabilidade.");
        s.setType("BEAR");
        s.setDiff(50);
        s.setBear(true);

        return s;
    }
}
