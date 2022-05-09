/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.shoukan;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.utils.json.JSONArray;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.EnumMap;

@Embeddable
public class CardAttributes implements Serializable {
	@Serial
	private static final long serialVersionUID = -8535846175709738591L;

	@Column(name = "mana", nullable = false)
	private int mana = 0;

	@Column(name = "blood", nullable = false)
	private int blood = 0;

	@Column(name = "atk", nullable = false)
	private int atk = 0;

	@Column(name = "def", nullable = false)
	private int def = 0;

	@Column(name = "dodge", nullable = false)
	private int dodge = 0;

	@Column(name = "block", nullable = false)
	private int block = 0;

	@Convert(converter = JSONArrayConverter.class)
	@Column(name = "tags", nullable = false)
	private JSONArray tags = new JSONArray();

	@ElementCollection
	@Column(name = "description", nullable = false, length = 140)
	@CollectionTable(name = "card_description")
	private EnumMap<I18N, String> description = new EnumMap<>(I18N.class);

	@Column(name = "effect", nullable = false, columnDefinition = "TEXT")
	private String effect;

	public String getDescription(I18N locale) {
		return description.getOrDefault(locale, "");
	}

	public String getEffect() {
		return effect;
	}

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

	public int getAtk() {
		return atk;
	}

	public void setAtk(int atk) {
		this.atk = atk;
	}

	public int getDef() {
		return def;
	}

	public void setDef(int def) {
		this.def = def;
	}

	public int getDodge() {
		return dodge;
	}

	public void setDodge(int dodge) {
		this.dodge = dodge;
	}

	public int getBlock() {
		return block;
	}

	public void setBlock(int block) {
		this.block = block;
	}

	public JSONArray getTags() {
		return tags;
	}

	public void setTags(JSONArray tags) {
		this.tags = tags;
	}
}
