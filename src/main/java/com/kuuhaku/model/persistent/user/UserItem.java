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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PassiveItemException;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "user_item")
public class UserItem extends DAO<UserItem> implements Comparable<UserItem> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Column(name = "icon", nullable = false)
	private String icon = "";

	@Column(name = "stack_size")
	private int stackSize;

	@Column(name = "price")
	private int price;

	@Enumerated(EnumType.STRING)
	@Column(name = "currency")
	private Currency currency;

	@Language("ShiroSig")
	@Column(name = "signature")
	private String signature;

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	@Column(name = "account_bound", nullable = false)
	private boolean accountBound = false;

	@OneToMany(cascade = ALL, orphanRemoval = true)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedItem> infos = new HashSet<>();

	public String getId() {
		return id;
	}

	public String getName(I18N locale) {
		return getInfo(locale).getName();
	}

	public String getDescription(I18N locale) {
		return getInfo(locale).getDescription();
	}

	public String getIcon() {
		return icon;
	}

	public int getStackSize() {
		return stackSize;
	}

	public int getPrice() {
		return price;
	}

	public Currency getCurrency() {
		return currency;
	}

	public boolean isPassive() {
		return effect == null;
	}

	public String getSignature() {
		return signature;
	}

	public void execute(I18N locale, GuildMessageChannel channel, Account acc, JSONObject params) {
		if (effect == null) throw new PassiveItemException();

		Utils.exec(effect, Map.of(
				"locale", locale,
				"channel", channel,
				"acc", acc,
				"params", params
		));
	}

	public boolean isAccountBound() {
		return accountBound;
	}

	public LocalizedItem getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale() == locale)
				.findAny().orElseThrow();
	}

	@Override
	public int compareTo(@NotNull UserItem o) {
		if (Objects.equals(this, o)) return 0;

		return Comparator.comparing(UserItem::getId).compare(this, o);
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
		return Objects.hashCode(id);
	}
}
