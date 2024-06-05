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

package com.kuuhaku.model.persistent.guild;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.RuleAction;
import com.kuuhaku.model.persistent.id.AutoRuleId;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Objects;

@Entity
@Table(name = "auto_rule")
public class AutoRule extends DAO<AutoRule> {
	@EmbeddedId
	private AutoRuleId id;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false)
	private RuleAction action;

	@Column(name = "threshold", nullable = false)
	private int threshold;

	@ManyToOne(optional = false)
	@JoinColumn(name = "gid", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("gid")
	private GuildSettings settings;

	public AutoRule() {
	}

	public AutoRule(GuildSettings settings, int threshold, RuleAction action) {
		this.id = new AutoRuleId(settings.getGid());
		this.threshold = threshold;
		this.action = action;
		this.settings = settings;
	}

	public AutoRuleId getId() {
		return id;
	}

	public RuleAction getAction() {
		return action;
	}

	public int getThreshold() {
		return threshold;
	}

	public GuildSettings getSettings() {
		return settings;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AutoRule autoRule = (AutoRule) o;
		return Objects.equals(id, autoRule.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
