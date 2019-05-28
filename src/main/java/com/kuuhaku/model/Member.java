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

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
public class Member {
    @Id
    private String id;
    private int level = 1, xp = 0;
    private String warns = "";
    private String bg = "";
    private String badges = "[false,false,false,false,false,false,false,false,false,false,false,false,false]";

    public Member() {

    }

    public boolean addXp() {
        xp += 15;
        if (xp >= (int) Math.pow(level, 2) * 100) {
            level++;
            return true;
        }
        return false;
    }

    public void resetXp() {
        level = 1;
        xp = 0;
    }

    public void giveBadge(String index) {
        List<Boolean> ph = new ArrayList<>();
        for (int i = 0; i < getBadges().length; i++) {
            ph.add(getBadges()[i]);
        }
        ph.set(Integer.parseInt(index), true);
        badges = ph.toString();
    }

    public void removeBadge(String index) {
        List<Boolean> ph = new ArrayList<>();
        for (int i = 0; i < getBadges().length; i++) {
            ph.add(getBadges()[i]);
        }
        ph.set(Integer.parseInt(index), false);
        badges = ph.toString();
    }

    public void addWarn(String reason) {
        List<String> ph = new ArrayList<>(Arrays.asList(getWarns()));
        ph.add(reason);
        warns = ph.toString();
    }

    public void removeWarn(int index) {
        List<String> ph = new ArrayList<>(Arrays.asList(getWarns()));
        ph.remove(index);
        warns = ph.toString();
    }

    public String getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    int getXp() {
        return xp;
    }

    private String[] getWarns() {
        return warns.replace("[", "").replace("]", "").split(",");
    }

    public boolean[] getBadges() {
        String[] t = badges.replace("[", "").replace("]", "").split(",");
        boolean[] tb = new boolean[t.length];
        for (int i = 0; i < t.length; i++) {
            tb[i] = Boolean.parseBoolean(t[i]);
        }
        return tb;
    }

    public void setId(String id) {
        this.id = id;
    }

    String getBg() {
        return bg;
    }

    public void setBg(String bg) {
        this.bg = bg;
    }
}
