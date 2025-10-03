package com.kuuhaku.model.persistent.shoukan;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CombatCardAttributes extends CardAttributes {
	@Column(name = "mana", nullable = false)
	private int mana = 0;

	@Column(name = "blood", nullable = false)
	private int blood = 0;

	@Column(name = "sacrifices", nullable = false)
	private int sacrifices = 0;

	@Column(name = "atk", nullable = false)
	private int atk = 0;

	@Column(name = "dfs", nullable = false)
	private int dfs = 0;

	@Column(name = "dodge", nullable = false)
	private int dodge = 0;

	@Column(name = "parry", nullable = false)
	private int parry = 0;

	public int getMana() {
		return mana;
	}

	public void setMana(int mana) {
		this.mana = mana;
	}

	public int getBlood() {
		return blood;
	}

	public void setBlood(int blood) {
		this.blood = blood;
	}

	public int getSacrifices() {
		return sacrifices;
	}

	public void setSacrifices(int sacrifices) {
		this.sacrifices = sacrifices;
	}

	public int getAtk() {
		return atk;
	}

	public void setAtk(int atk) {
		this.atk = atk;
	}

	public int getDfs() {
		return dfs;
	}

	public void setDfs(int dfs) {
		this.dfs = dfs;
	}

	public int getDodge() {
		return dodge;
	}

	public void setDodge(int dodge) {
		this.dodge = dodge;
	}

	public int getParry() {
		return parry;
	}

	public void setParry(int parry) {
		this.parry = parry;
	}

	public CombatCardAttributes copy() {
		return (CombatCardAttributes) super.copy();
	}
}
