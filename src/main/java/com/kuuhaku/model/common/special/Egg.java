package com.kuuhaku.model.common.special;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.util.Calc;
import org.apache.commons.collections4.bag.HashBag;

import java.util.List;

public record Egg(int cr, HashBag<UserItem> items) {
	public static Egg random() {
		RandomList<UserItem> rl = new RandomList<>();
		List<UserItem> pool = DAO.queryAll(UserItem.class, "SELECT i FROM UserItem i WHERE i.accountBound = FALSE AND i.currency IS NOT NULL");

		for (UserItem i : pool) {
			rl.add(i, switch (i.getCurrency()) {
				case CR -> 1000;
				case ITEM -> 750;
				case GEM -> 300;
			});
		}

		HashBag<UserItem> items = new HashBag<>();
		while (Calc.chance(100d / items.size())) {
			items.add(rl.get());
		}

		return new Egg(Calc.rng(4000, 8000), items);
	}
}
