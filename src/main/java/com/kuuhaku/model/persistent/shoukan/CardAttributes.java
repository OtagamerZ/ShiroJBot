/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.persistent.localized.LocalizedDescription;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.intellij.lang.annotations.Language;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.ALL;

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

	@Column(name = "parry", nullable = false)
	private int parry = 0;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "tags", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray tags = new JSONArray();

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "card_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedDescription> descriptions = new HashSet<>();

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	@Transient
	private transient EnumSet<Trigger> lock = EnumSet.noneOf(Trigger.class);

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

	public JSONArray getTags() {
		return tags;
	}

	public Set<LocalizedDescription> getDescriptions() {
		return descriptions;
	}

	public String getDescription(I18N locale) {
		return descriptions.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(LocalizedDescription::toString)
				.findAny().orElse("");
	}

	public void appendDescription(I18N locale, String description) {
		LocalizedDescription desc = descriptions.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.findAny()
				.orElse(new LocalizedDescription(locale, ""));

		if (!desc.getDescription().contains(description)) {
			desc.setDescription(desc.getDescription() + " " + description);
			descriptions.add(desc);
		}
	}

	public String getEffect() {
		return Utils.getOr(effect, "");
	}

	public void setEffect(@Language("Groovy") String effect) {
		this.effect = effect;
	}

	public void appendEffect(@Language("Groovy") String effect) {
		this.effect = Utils.getOr(this.effect, "");

		Set<String> imports = new HashSet<>();
		String code = effect.lines()
				.dropWhile(l -> {
					String trim = l.trim();

					if (trim.isBlank()) return true;
					else if (trim.startsWith("import")) {
						imports.add(trim);
						return true;
					}

					return false;
				})
				.map(l -> "\t" + l)
				.collect(Collectors.joining("\n"));

		imports.removeIf(i -> Utils.match(this.effect, "^" + i));

		if (imports.isEmpty()) {
			this.effect += "{\n" + code + "\n}";
		} else {
			this.effect = String.join("\n", imports) + "\n" + this.effect;
		}
	}

	public EnumSet<Trigger> getLocks() {
		return lock;
	}

	public boolean isLocked(Trigger trigger) {
		return lock.contains(trigger);
	}

	public void lock(Trigger... triggers) {
		if (triggers.length == 0) throw new IllegalArgumentException("Triggers cannot be empty");
		for (Trigger trigger : triggers) {
			if (trigger == null) continue;

			lock.add(trigger);
		}
	}

	public void lock(Set<Trigger> triggers) {
		lock.addAll(triggers);
	}

	public void lockAll() {
		lock.addAll(EnumSet.allOf(Trigger.class));
	}

	public void unlock(Trigger... triggers) {
		if (triggers.length == 0) throw new IllegalArgumentException("Triggers cannot be empty");
		for (Trigger trigger : triggers) {
			if (trigger == null) continue;

			lock.remove(trigger);
		}
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
