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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Entity
public class Beyblade {
    @Id
    private String id;
    private String name = "";
    private String color = "#ffffff";
    private float speed = 1.0f, strength = 1.0f, stability = 1.5f;
    private int life = 100, wins = 0, loses = 0, points = 0;
    private int special;
    private String voteTime;
    transient private Special s;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public float getSpeed() {
        return speed;
    }

    public void addSpeed() {
        this.speed += 0.5f;
    }

    public float getStrength() {
        return strength;
    }

    public void addStrength() {
        this.strength += 0.5f;
    }

    public float getStability() {
        return stability;
    }

    public void addStability() {
        this.stability += 0.5f;
    }

    public int getLife() {
        return life;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public void addLife() {
        this.life += 50;
    }

    public int getWins() {
        return wins;
    }

    public void addWins() {
        wins += 1;
    }

    public int getLoses() {
        return loses;
    }

    public void addLoses() {
        loses += 1;
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int points) {
        this.points += points;
    }

    public void takePoints(int points) {
        this.points -= points;
    }

    public float getKDA() {
        return (float) wins / (loses == 0 ? 0.5f : loses);
    }

    public Special getS() {
        return s;
    }

    public void setS(Special s) {
        this.s = s;
    }

    public int getSpecial() {
        return special;
    }

    public void setSpecial(int special) {
        this.special = special;
    }

    public boolean hasVoted() {
    	try {
			return System.currentTimeMillis() - Long.parseLong(voteTime) < 43200000;
		} catch (Exception e) {
    		return false;
		}
    }

    public String getVoteTime() {
    	long time = 43200000 - (System.currentTimeMillis() - Long.parseLong(voteTime));
		long hours = TimeUnit.MILLISECONDS.toHours(time);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(time) - (TimeUnit.MILLISECONDS.toHours(time)* 60);

		return hours + " horas e " + minutes + " minutos!";
	}

    public void setVoteTime(Long voteTime) {
        this.voteTime = voteTime.toString();
    }
}
