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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.ItemException;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.id.LocalizedId;
import com.kuuhaku.model.persistent.shoukan.LocalizedString;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import jakarta.persistence.*;
import org.intellij.lang.annotations.Language;

import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "user_item")
public class UserItem extends DAO<UserItem> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Column(name = "icon")
	private String icon;

	@Column(name = "price")
	private int price;

	@Enumerated(EnumType.STRING)
	@Column(name = "currency")
	private Currency currency;

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT", nullable = false)
	private String effect;

	public String getId() {
		return id;
	}

	public String getIcon() {
		return icon;
	}

	public int getPrice() {
		return price;
	}

	public Currency getCurrency() {
		return currency;
	}

	public boolean execute(Account acc, JSONObject args) {
		try {
			Utils.exec(effect, Map.of(
					"acc", acc,
					"args", args
			));

			return true;
		} catch (ItemException e) {
			return false;
		}
	}

	public String toString(I18N locale) {
		LocalizedString ls = DAO.find(LocalizedString.class, new LocalizedId("item/" + id, locale));
		if (ls == null) {
			return icon + " " + id;
		}

		return icon + " " + ls.getValue();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserItem userItem = (UserItem) o;
		return Objects.equals(id, userItem.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
