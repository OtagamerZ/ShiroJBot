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
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONArray;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

import java.io.Serial;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Embeddable
public class CardAttributes implements Serializable, Cloneable {
	@Serial
	private static final long serialVersionUID = -8535846175709738591L;

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

	@Column(name = "block", nullable = false)
	private int block = 0;

	@Type(JsonBinaryType.class)
	@Column(name = "tags", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray tags = new JSONArray();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "id", referencedColumnName = "card_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedDescription> descriptions = new HashSet<>();

	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	private transient EnumSet<Trigger> lock = EnumSet.noneOf(Trigger.class);

	public int getMana() {
		return mana;
	}

	public int getBlood() {
		return blood;
	}

	public int getSacrifices() {
		return sacrifices;
	}

	public int getAtk() {
		return atk;
	}

	public int getDfs() {
		return dfs;
	}

	public int getDodge() {
		return dodge;
	}

	public int getBlock() {
		return block;
	}

	public JSONArray getTags() {
		return tags;
	}

	public String getDescription(I18N locale) {
		for (LocalizedDescription ld : descriptions) {
			if (ld.getLocale() == locale) {
				return ld.getDescription();
			}
		}

		return "";
	}

	public String getEffect() {
		return Utils.getOr(effect, "");
	}

	public EnumSet<Trigger> getLocks() {
		return lock;
	}

	public boolean isLocked(Trigger trigger) {
		return lock.contains(trigger);
	}

	public void lock(Trigger... triggers) {
		if (triggers.length == 0) throw new IllegalArgumentException("Triggers cannot be empty");

		lock(Set.of(triggers));
	}

	public void lock(Set<Trigger> triggers) {
		lock.addAll(triggers);
	}

	public void lockAll() {
		lock.addAll(EnumSet.allOf(Trigger.class));
	}

	public void unlock(Trigger... triggers) {
		if (triggers.length == 0) throw new IllegalArgumentException("Triggers cannot be empty");

		unlock(Set.of(triggers));
	}

	public void unlock(Set<Trigger> triggers) {
		lock.removeAll(triggers);
	}

	public void unlockAll() {
		lock.clear();
	}

	@Override
	public CardAttributes clone() throws CloneNotSupportedException {
		CardAttributes clone = (CardAttributes) super.clone();
		clone.tags = new JSONArray(tags);
		clone.descriptions = new HashSet<>(descriptions);

		return clone;
	}
}
