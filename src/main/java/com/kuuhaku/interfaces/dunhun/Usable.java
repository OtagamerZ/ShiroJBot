package com.kuuhaku.interfaces.dunhun;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.UsableStats;

public interface Usable {
	String getId();

	UsableStats getStats();

	boolean execute(Dunhun game, Actor<?> source, Actor<?> target);

	boolean isLocked();

	void setLocked(boolean locked);

	Usable copyWith(double efficiency, double critical);
}
