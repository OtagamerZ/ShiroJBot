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

package com.kuuhaku.model.persistent.shiro;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.util.Utils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.intellij.lang.annotations.Language;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "giftcode")
public class Giftcode extends DAO<Giftcode> {
	@Id
	@Column(name = "code", nullable = false, length = 32)
	private String code;

	@Column(name = "redeemer")
	private String redeemer;

	@Column(name = "description")
	private String description;

	@Language("Groovy")
	@Column(name = "gift", columnDefinition = "TEXT")
	private String gift;

	@Column(name = "used_at")
	private ZonedDateTime usedAt = null;

	public String getCode() {
		return code;
	}

	public String getRedeemer() {
		return redeemer;
	}

	public String getDescription() {
		return description;
	}

	public ZonedDateTime getUsedAt() {
		return usedAt;
	}

	public boolean redeem(Account acc) {
		try {
			Utils.exec(gift, Map.of("acc", acc));
			redeemer = acc.getUid();
			usedAt = ZonedDateTime.now(ZoneId.of("GMT-3"));
			save();

			return true;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute giftcode " + code);
			return false;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Giftcode giftcode = (Giftcode) o;
		return Objects.equals(code, giftcode.code);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code);
	}
}
