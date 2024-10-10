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
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.localized.LocalizedEvent;
import com.kuuhaku.model.persistent.localized.LocalizedString;
import com.kuuhaku.model.records.dunhun.EventAction;
import com.kuuhaku.model.records.dunhun.EventDescription;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.apache.commons.text.WordUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "event", schema = "dunhun")
public class Event extends DAO<Event> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedEvent> infos = new HashSet<>();

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	private transient final Map<String, Supplier<String>> actions = new HashMap<>();
	private transient String result;
	private transient I18N locale;

	public String getId() {
		return id;
	}

	public LocalizedEvent getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public EventDescription parse(I18N locale, Dunhun game) {
		String desc = getInfo(locale).getDescription();
		this.locale = locale;

		List<EventAction> out = new ArrayList<>();
		desc = Utils.regex(desc, "\\[(.+?)]\\{(.+?)}").replaceAll(m -> {
			out.add(new EventAction(WordUtils.capitalizeFully(m.group(1)), m.group(2)));
			return Matcher.quoteReplacement("**" + m.group(1) + "**");
		});

		try {
			Utils.exec(id, effect, Map.of(
					"locale", locale,
					"dungeon", game,
					"event", this
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute event {}", id, e);
		}

		return new EventDescription(game.parsePlural(desc), out);
	}

	public void forAction(String action, Supplier<String> act) {
		actions.put(action, act);
	}

	public Supplier<String> getAction(String action) {
		return actions.getOrDefault(action, () -> "");
	}

	public String getEvent(Dunhun game, String key) {
		return game.parsePlural(LocalizedString.get(locale, "event/" + key));
	}

	public String getOutcome(String key, Object... args) {
		return LocalizedString.get(locale, "outcome/" + key).formatted(args);
	}

	public String getEvent(Dunhun game, String key, String outcome, Object... args) {
		return getEvent(game, key) + "\n\n" + getOutcome(outcome, args);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Event affix = (Event) o;
		return Objects.equals(id, affix.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	public static Event getRandom() {
		return DAO.query(Event.class, "SELECT e FROM Event e ORDER BY random()");
	}
}
