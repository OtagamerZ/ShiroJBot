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

import bsh.EvalError;
import bsh.Interpreter;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.utils.Helper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "giftcode")
public class GiftCode {
	@Id
	@Column(columnDefinition = "CHAR(32) NOT NULL")
	private String code;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String redeemedBy = "";

	@Column(columnDefinition = "VARCHAR(100) NOT NULL DEFAULT ''")
	private String description = "";

	@Column(columnDefinition = "TEXT NOT NULL DEFAULT ''")
	private String gift = "";

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime redeemed = null;

	public GiftCode(String code) {
		this.code = code;
	}

	public GiftCode() {
	}

	public String getCode() {
		return code;
	}

	public String getRedeemedBy() {
		return redeemedBy;
	}

	public void setRedeemedBy(String redeemedBy) {
		this.redeemedBy = redeemedBy;
		this.redeemed = ZonedDateTime.now(ZoneId.of("GMT-3"));
		;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGift() {
		return gift;
	}

	public void setGift(String gift) {
		this.gift = gift;
	}

	public void useCode(String id) {
		String imports = """
				//%s
				import com.kuuhaku.model.controller.postgresql.*;
				import com.kuuhaku.model.persistent.*;
				import com.kuuhaku.utils.*;
								
				          """.formatted(code);

		Account acc = AccountDAO.getAccount(id);

		try {
			Interpreter i = new Interpreter();
			i.setStrictJava(true);
			i.set("acc", acc);
			i.eval(imports + gift);
			AccountDAO.saveAccount(acc);
		} catch (EvalError e) {
			Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
		}
	}

	public ZonedDateTime getRedeemed() {
		return redeemed;
	}

	public void setRedeemed(ZonedDateTime redeemed) {
		this.redeemed = redeemed;
	}
}
