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

import javax.persistence.*;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

@Entity
@Table(name = "giftcode")
public class GiftCode {
	@Id
	@Column(columnDefinition = "CHAR(32) NOT NULL")
	private String code;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String redeemedBy = "";

	@Column(columnDefinition = "TEXT NOT NULL DEFAULT ''")
	private String gift = "";

	@Temporal(TemporalType.DATE)
	private Calendar redeemed = null;

	public GiftCode(String code) {
		this.code = code;
	}

	public GiftCode() {
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getRedeemedBy() {
		return redeemedBy;
	}

	public void setRedeemedBy(String redeemedBy) {
		this.redeemedBy = redeemedBy;
		this.redeemed = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT-3")));
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
				import com.kuuhaku.model.persistent.Account;
				import com.kuuhaku.model.persistent.GiftCode;
								
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
}
