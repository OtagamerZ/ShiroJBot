/*
 * This file is part of Shiro J Bot.
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

import org.json.JSONArray;

import javax.persistence.*;

@Entity
@Table(name = "quiz")
public class Quiz {
	public static final String[] OPTS = {"\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8", "\uD83C\uDDE9"};

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "TEXT NOT NULL")
	private String question = "";

	@Column(columnDefinition = "TEXT NOT NULL")
	private String options = "[]";

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int prize = 0;

	public Quiz(String question, JSONArray options, int prize) {
		this.question = question;
		this.options = options.toString();
		this.prize = prize;
	}

	public Quiz() {
	}

	public int getId() {
		return id;
	}

	public String getQuestion() {
		return question;
	}

	public JSONArray getOptions() {
		return new JSONArray(options);
	}

	public int getPrize() {
		return prize;
	}
}
