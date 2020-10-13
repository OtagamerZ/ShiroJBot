/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent;

import com.kuuhaku.controller.postgresql.GuildBuffDAO;
import com.kuuhaku.utils.ServerBuff;
import com.kuuhaku.utils.ShiroInfo;
import org.json.JSONArray;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Entity
@Table(name = "guildbuff")
public class GuildBuff {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String id;

	@Column(columnDefinition = "TEXT")
	private String buffs = "[]";

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
		if (buffs == null || buffs.isBlank()) return null;
		return getBuffs().stream().filter(b -> b.getId() == id).findFirst().orElse(null);
	}

	public Set<ServerBuff> getBuffs() {
		if (buffs == null || buffs.isBlank()) {
			setBuffs(new HashSet<>());
			return new HashSet<>();
		}
		HashSet<ServerBuff> sb = new JSONArray(buffs).toList().stream().map(b -> ShiroInfo.getJSONFactory().create().fromJson((String) b, ServerBuff.class)).collect(Collectors.toCollection(HashSet::new));
		HashSet<ServerBuff> toRemove = sb.stream().filter(b ->
				b.getTier() == 4 ?
						TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - b.getAcquiredAt()) > b.getTime() :
						TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - b.getAcquiredAt()) > b.getTime())
				.collect(Collectors.toCollection(HashSet::new));
		sb.removeAll(toRemove);
		setBuffs(sb);
		if (toRemove.size() > 0) GuildBuffDAO.saveBuffs(this);
		return sb;
	}

	public boolean addBuff(ServerBuff buff) {
		Set<ServerBuff> sb = getBuffs();
		if (sb.stream().anyMatch(buff::equals)) return false;

		sb.add(buff);
		setBuffs(sb);
		return true;
	}

	public void setBuffs(Set<ServerBuff> buffs) {
		List<String> sb = buffs.stream().map(b -> ShiroInfo.getJSONFactory().create().toJson(b)).collect(Collectors.toList());
		this.buffs = new JSONArray(sb).toString();
	}
}
