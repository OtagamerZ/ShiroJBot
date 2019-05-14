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

    public int getXp() {
        return xp;
    }

    public String[] getWarns() {
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

    private void setId(String id) {
        this.id = id;
    }
}
