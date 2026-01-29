package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.dunhun.Node;
import com.kuuhaku.model.persistent.dunhun.Event;

import java.util.concurrent.atomic.AtomicReference;

public class EventContext extends EffectContext<Event> {
	private final Node node;
	private final AtomicReference<String> description;

	public EventContext(Dunhun game, Event event, Node node, String description) {
		super(game, event);
		this.node = node;
		this.description = new AtomicReference<>(description);
	}

	public Node getNode() {
		return node;
	}

	public AtomicReference<String> getDescription() {
		return description;
	}
}
