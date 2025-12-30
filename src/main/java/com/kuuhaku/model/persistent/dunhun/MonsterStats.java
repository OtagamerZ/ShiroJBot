/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.model.common.dunhun.MonsterBase;
import com.kuuhaku.model.common.dunhun.context.LootContext;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.records.dunhun.Loot;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.intellij.lang.annotations.Language;

import java.io.Serializable;
import java.util.Map;

@Embeddable
public class MonsterStats implements Serializable {
	@Column(name = "base_hp", nullable = false)
	private int baseHp;

	@Enumerated(EnumType.STRING)
	@Column(name = "race", nullable = false)
	private Race race;

	@Column(name = "attack", nullable = false)
	private int attack;

	@Column(name = "defense", nullable = false)
	private int defense;

	@Column(name = "dodge", nullable = false)
	private int dodge;

	@Column(name = "parry", nullable = false)
	private int parry;

	@Column(name = "action_points", nullable = false)
	private int maxAp;

	@Column(name = "initiative", nullable = false)
	private int initiative;

	@Column(name = "kill_xp", nullable = false)
	private int killXp;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "skills", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray skills = new JSONArray();

	@Language("Groovy")
	@Column(name = "loot_generator", columnDefinition = "TEXT")
	private String lootGenerator;

	public MonsterStats() {
	}

	public MonsterStats(int baseHp, Race race, int attack, int defense, int dodge, int parry, int maxAp, int initiative, int killXp) {
		this.baseHp = baseHp;
		this.race = race;
		this.attack = attack;
		this.defense = defense;
		this.dodge = dodge;
		this.parry = parry;
		this.maxAp = maxAp;
		this.initiative = initiative;
		this.killXp = killXp;
	}

	public int getBaseHp() {
		return baseHp;
	}

	public Race getRace() {
		return race;
	}

	public int getAttack() {
		return attack;
	}

	public int getDefense() {
		return defense;
	}

	public int getDodge() {
		return dodge;
	}

	public int getParry() {
		return parry;
	}

	public JSONArray getSkills() {
		return skills;
	}

	public int getMaxAp() {
		return maxAp;
	}

	public int getInitiative() {
		return initiative;
	}

	public int getKillXp() {
		return killXp;
	}

	public double getLootMultiplier(MonsterBase<?> self) {
		if (self == null || self.isMinion()) return 0;
		else if (self.getRarityClass() == RarityClass.UNIQUE) {
			return 2.5;
		} else if (self instanceof Monster m) {
			return 1 + m.getAffixes().size() * 0.15;
		}

		return 1;
	}

	public Loot generateLoot(MonsterBase<?> self) {
		Loot loot = new Loot();
		if (!(self instanceof MonsterBase<?> m) || m.isMinion() || lootGenerator == null) return loot;

		try {
			Utils.exec(getClass().getSimpleName(), lootGenerator, Map.of(
					"ctx", new LootContext(self, loot, getLootMultiplier(self))
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to generate loot for {}", m.getName(I18N.EN), e);
		}

		return loot;
	}
}
