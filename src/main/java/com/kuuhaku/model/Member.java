package com.kuuhaku.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Arrays;

@Entity
public class Member implements Serializable {
    @Id
    private String id;
    private int level = 1, xp = 0;
    private String warns = "";
    private boolean[] badges = {false,false,false,false,false,false,false,false,false,false,false,false,false};

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
            boolean [] ph = getBadges();
            ph[Integer.parseInt(index)] = true;
            setBadges(ph);
        }
    }

    public void removeBadge(String index) {
        if (Integer.parseInt(index) >= 0 && Integer.parseInt(index) < getBadges().length) {
            boolean [] ph = getBadges();
            ph[Integer.parseInt(index)] = false;
            setBadges(ph);
        }
    }

    public String getId() {
        return id;
    }

    public int getLevel() {
        return level;
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
        return badges;
    }

    private void setBadges(boolean[] badges) {
        this.badges = badges;
    }

    private void setId(String id) {
        this.id = id;
    }
}
