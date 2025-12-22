package com.kuuhaku.interfaces.dunhun;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.SkillStats;

public interface Usable {
	String getId();

	SkillStats getStats();

	boolean execute(Dunhun game, Actor<?> source, Actor<?> target);

	Usable copyWith(double efficiency, double critical);
}
