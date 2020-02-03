/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.handlers.games.rpg.entities;

public class Status {
	private boolean alive = true;
	private int xp = 0;
	private int life = 100;
	private int defense = 1;
	private int strength = 1;
	private int perception = 1;
	private int endurance = 1;
	private int charisma = 1;
	private int intelligence = 1;
	private int agility = 1;
	private int luck = 1;
	private final int[] baseStats;

	Status(int strength, int perception, int endurance, int charisma, int intelligence, int agility, int luck) {
		this.strength += strength;
		this.perception += perception;
		this.endurance += endurance;
		this.charisma += charisma;
		this.intelligence += intelligence;
		this.agility += agility;
		this.luck += luck;
		baseStats = new int[]{this.defense, this.strength, this.perception, this.endurance, this.charisma, this.intelligence, this.agility, this.luck};
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public int getLevel() {
		return (int) Math.floor(Math.sqrt((xp + 100) / 100f));
	}

	public int[] getBaseStats() {
		return baseStats;
	}

	public void damage(int damage) {
		modifyLife(damage);
		if (getLife() <= 0) setAlive(false);
		else setAlive(true);
	}

	public void addXp(int xp) {
		this.xp += xp;
	}

	public int getXp() {
		return xp;
	}

	public void setStats(int[] stats) {
		this.defense = stats[0];
		this.strength = stats[1];
		this.perception = stats[2];
		this.endurance = stats[3];
		this.charisma = stats[4];
		this.intelligence = stats[5];
		this.agility = stats[6];
		this.luck = stats[7];
	}

	public int getLife() {
		return life;
	}

	public void modifyLife(int life) {
		this.life += life;
	}

	public int getDefense() {
		return defense;
	}

	public void setDefense(int defense) {
		this.defense = defense;
	}

	public int getStrength() {
		return strength;
	}

	public void setStrength(int strength) {
		this.strength = strength;
	}

	public int getPerception() {
		return perception;
	}

	public void setPerception(int perception) {
		this.perception = perception;
	}

	public int getEndurance() {
		return endurance;
	}

	public void setEndurance(int endurance) {
		this.endurance = endurance;
	}

	public int getCharisma() {
		return charisma;
	}

	public void setCharisma(int charisma) {
		this.charisma = charisma;
	}

	public int getIntelligence() {
		return intelligence;
	}

	public void setIntelligence(int intelligence) {
		this.intelligence = intelligence;
	}

	public int getAgility() {
		return agility;
	}

	public void setAgility(int agility) {
		this.agility = agility;
	}

	public int getLuck() {
		return luck;
	}

	public void setLuck(int luck) {
		this.luck = luck;
	}
}
