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

import com.kuuhaku.model.common.XStringBuilder;
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
@MappedSuperclass
public class CardAttributes implements Serializable, Cloneable {
	@Serial
	private static final long serialVersionUID = -8535846175709738591L;

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

	@Transient
	private transient Set<String> effects = new HashSet<>();

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
		if (!effects.isEmpty()) {
			Set<String> imports = new HashSet<>();

			XStringBuilder sb = new XStringBuilder();
			for (String e : effects) {
				String code = e.lines()
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

				sb.appendNewLine("{\n" + code + "\n}");
				if (this instanceof CombatCardAttributes cca) {
					cca.setMana(cca.getMana() + 1);
				}
			}

			if (imports.isEmpty()) {
				effect = sb.toString();
			} else {
				effect = String.join("\n", imports) + "\n" + sb;
			}

			effects.clear();
		}

		return Utils.getOr(effect, "");
	}

	public void setEffect(@Language("Groovy") String effect) {
		this.effect = effect;
	}

	public void appendEffect(@Language("Groovy") String effect) {
		effects.add(effect);
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

	public CardAttributes copy() {
		try {
			CardAttributes clone = (CardAttributes) super.clone();
			clone.tags = new JSONArray(tags);
			clone.descriptions = new HashSet<>(descriptions);

			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
}
