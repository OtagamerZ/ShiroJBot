package com.kuuhaku.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Arrays;

@Entity
public class Member {
    @Id
    private String id;
    private int level = 1, xp = 0;
    private String warns = "";
    private String badges = "[false,false,false,false,false,false,false,false,false,false,false,false,false]";

    public Member(String id) {
        setId(id);
    }

    public boolean addXp() {
        xp += 15;
        if (xp >= level * (100 * level)) {
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
        if (Integer.parseInt(index) >= 0 && Integer.parseInt(index) < getBadges().length) {
            getBadges()[Integer.parseInt(index)] = true;
        }
    }

    public void removeBadge(String index) {
        if (Integer.parseInt(index) >= 0 && Integer.parseInt(index) < getBadges().length) {
            getBadges()[Integer.parseInt(index)] = false;
        }
    }

    public String getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public String[] getWarns() {
        return warns.replace("[", "").replace("]", "").split(",");
    }

    public void setWarns(String[] warns) {
        this.warns = Arrays.toString(warns);
    }

    public boolean[] getBadges() {
        String[] t = badges.replace("[", "").replace("]", "").split(",");
        boolean[] tb = new boolean[t.length];
        for (int i = 0; i < t.length; i++) {
            tb[i] = Boolean.parseBoolean(t[0]);
        }
        return tb;
    }

    public void setBadges(boolean[] badges) {
        this.badges = Arrays.toString(badges);
    }

    private void setId(String id) {
        this.id = id;
    }
}
