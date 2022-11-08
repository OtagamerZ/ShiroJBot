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

package com.kuuhaku.model.persistent.guild;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.id.CustomAnswerId;
import com.kuuhaku.util.json.JSONArray;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

import java.util.Objects;

@Entity
@Table(name = "custom_answer")
public class CustomAnswer extends DAO<CustomAnswer> {
	@EmbeddedId
	private CustomAnswerId id;

	@Column(name = "author", nullable = false)
	private String author;

	@Column(name = "trigger", nullable = false)
	private String trigger;

	@Column(name = "answer", nullable = false)
	private String answer;

	@Column(name = "chance", nullable = false)
	private int chance = 100;

	@Type(JsonBinaryType.class)
	@Column(name = "channels", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray channels = new JSONArray();

	@Type(JsonBinaryType.class)
	@Column(name = "users", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray users = new JSONArray();

	@ManyToOne(optional = false)
	@JoinColumn(name = "gid", nullable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("gid")
	private GuildSettings settings;

	public CustomAnswer() {
	}

	public CustomAnswer(GuildSettings settings, String author, String trigger, String answer, int chance, JSONArray channels, JSONArray users) {
		this.id = new CustomAnswerId(settings.getGid());
		this.author = author;
		this.trigger = trigger;
		this.answer = answer;
		this.chance = chance;
		this.channels = channels;
		this.users = users;
		this.settings = settings;
	}

	public CustomAnswerId getId() {
		return id;
	}

	public String getAuthor() {
		return author;
	}

	public String getTrigger() {
		return trigger;
	}

	public String getAnswer() {
		return answer;
	}

	public int getChance() {
		return chance;
	}

	public JSONArray getChannels() {
		return channels;
	}

	public JSONArray getUsers() {
		return users;
	}

	public void setSettings(GuildSettings settings) {
		this.settings = settings;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CustomAnswer that = (CustomAnswer) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
