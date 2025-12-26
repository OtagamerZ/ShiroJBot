package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.persistent.dunhun.Event;

public class EventContext extends EffectContext<Event> {
	public EventContext(Dunhun game, Event event) {
		super(game, event);
	}
}
