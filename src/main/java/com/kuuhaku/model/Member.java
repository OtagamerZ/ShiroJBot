package com.kuuhaku.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Member {
    @Id
    private String id;
    private int level = 1, xp = 0;
    private String[] warns = {};
    private boolean[] badges = {false, false, false, false, false, false, false, false, false, false, false, false};

    public Member(String id) {
        setId(id);
    }

    public boolean addXp() {
        xp += 15;
        if (xp >= level * 100) {
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
        if (Integer.parseInt(index) >= 0 && Integer.parseInt(index) < badges.length) {
            this.badges[Integer.parseInt(index)] = true;
        }
    }

    public void removeBadge(String index) {
        if (Integer.parseInt(index) >= 0 && Integer.parseInt(index) < badges.length) {
            this.badges[Integer.parseInt(index)] = false;
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
        return warns;
    }

    public void setWarns(String[] warns) {
        this.warns = warns;
    }

    public boolean[] getBadges() {
        return badges;
    }

    public void setBadges(boolean[] badges) {
        this.badges = badges;
    }

    private void setId(String id) {
        this.id = id;
    }
}
