package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.persistent.dunhun.Event;

public class EventContext extends EffectContext {
	private final Event event;

	public EventContext(Dunhun game, Event event) {
		super(game);
		this.event = event;
	}

	public Event getEvent() {
		return event;
	}
}
