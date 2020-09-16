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

package com.kuuhaku.model.enums;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.PStateDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.ExceedMember;

public enum CreditLoan {
	LOAN_1(1000, 0.1),
	LOAN_2(2500, 0.3),
	LOAN_3(5000, 0.5),
	LOAN_4(10000, 1),
	LOAN_5(25000, 2);

	private final int loan;
	private final double interest;

	CreditLoan(int loan, double interest) {
		this.loan = loan;
		this.interest = interest;
	}

	public static CreditLoan getById(int id) {
		return valueOf("LOAN_" + id);
	}

	public int getLoan() {
		return loan;
	}

	public double getInterest(ExceedMember ex) {
		return 1 + interest * (ex == null ? 1 : (1 - PStateDAO.getInfluenceShare(ExceedEnum.getByName(ex.getExceed()))));
	}

	public void sign(Account acc) {
		acc.signLoan(this);
		AccountDAO.saveAccount(acc);
	}
}
