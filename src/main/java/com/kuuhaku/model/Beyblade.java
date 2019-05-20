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

import org.json.JSONObject;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.awt.*;

@Entity
public class Beyblade {
    @Id
    private String id;
    private String name = "";
    private String defs = "{\"speed\": 1, \"strength\": 1, \"stability\": 1, \"color\": \"#fff\", \"wins\": 0, \"loses\": 0}";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JSONObject getDefs() {
        return new JSONObject(defs);
    }

    public void setSpeed(Double speed) {
        JSONObject defs = new JSONObject(this.defs);
        defs.put("speed", speed);
        this.defs = defs.toString();
    }

    public void setStrength(Double str) {
        JSONObject defs = new JSONObject(this.defs);
        defs.put("strength", str);
        this.defs = defs.toString();
    }

    public void setStability(Double stb) {
        JSONObject defs = new JSONObject(this.defs);
        defs.put("stability", stb);
        this.defs = defs.toString();
    }

    public void addWin() {
        JSONObject defs = new JSONObject(this.defs);
        int wins = defs.getInt("wins");
        defs.put("wins", wins + 1);
        this.defs = defs.toString();
    }

    public void addLose() {
        JSONObject defs = new JSONObject(this.defs);
        int loses = defs.getInt("loses");
        defs.put("loses", loses + 1);
        this.defs = defs.toString();
    }

    public void setColor(Color clr) {
        JSONObject defs = new JSONObject(this.defs);
        defs.put("color", clr.toString());
        this.defs = defs.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
