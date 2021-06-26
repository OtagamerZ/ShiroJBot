/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.controller.postgresql.GuildBuffDAO;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "guildbuff")
public class GuildBuff {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String id;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "guildconfig_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<ServerBuff> buffs = new HashSet<>();

	public String getId() {
		return id;
	}

	public GuildBuff(String id) {
		this.id = id;
	}

	public GuildBuff() {
	}

	public void setId(String id) {
		this.id = id;
	}

	public ServerBuff getBuff(int id) {
		return buffs.stream()
				.filter(b -> b.getType().ordinal() == id)
				.findFirst().orElse(null);
	}

	public Set<ServerBuff> getBuffs() {
		if (buffs.removeIf(b -> System.currentTimeMillis() - b.getAcquiredAt() > b.getTime()))
			GuildBuffDAO.saveBuffs(this);

		return buffs;
	}

	public boolean addBuff(ServerBuff buff) {
		if (buffs.stream().anyMatch(b -> buff.getType() == b.getType() && buff.getTier() < b.getTier())) return false;

		buffs.remove(buff);
		buffs.add(buff);
		return true;
	}

	public void setBuffs(Set<ServerBuff> buffs) {
		this.buffs = buffs;
	}
}
