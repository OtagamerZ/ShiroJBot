package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.dunhun.Node;
import com.kuuhaku.model.persistent.dunhun.Event;

public class EventContext extends EffectContext<Event> {
	private final Node node;

	public EventContext(Dunhun game, Event event, Node node) {
		super(game, event);
		this.node = node;
	}

	public Node getNode() {
		return node;
	}
}
