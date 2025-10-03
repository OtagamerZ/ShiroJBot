package com.kuuhaku.interfaces;

import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Title;

import java.util.List;

public interface TitleLocked {
	String getId();

	List<Title> getTitles();

	default boolean canUse(Account acc) {
		return canUse(acc, 0);
	}

	default boolean canUse(Account acc, int price) {
		if (acc == null || getTitles() == null) return true;

		for (Object title : getTitles()) {
			if (title instanceof String s) {
				if (!acc.hasTitle(s)) return false;
			}
		}

		if (price > 0) {
			return !acc.getDynValue("ss_" + getId().toLowerCase()).isBlank();
		}

		return true;
	}
}
