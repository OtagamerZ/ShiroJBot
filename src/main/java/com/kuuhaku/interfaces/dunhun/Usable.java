package com.kuuhaku.interfaces.dunhun;

import com.kuuhaku.model.persistent.dunhun.SkillStats;

public interface Usable {
	String getId();

	SkillStats getStats();

	Usable copyWith(double efficiency, double critical);
}
