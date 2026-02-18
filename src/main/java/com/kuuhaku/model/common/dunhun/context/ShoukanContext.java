package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.shoukan.EffectParameters;

import java.util.Map;

public class ShoukanContext extends EffectContext<Drawable<?>> {
	private final Trigger trigger;
	private final EffectParameters params;
	private final Shoukan shoukan;
	private final Senshi self;
	private final Side side;
	private final Map<String, Object> data;

	public ShoukanContext(Drawable<?> source, Trigger trigger, EffectParameters params, Shoukan game, Senshi self, Side side, Map<String, Object> data) {
		super(null, source);
		this.trigger = trigger;
		this.params = params;
		this.shoukan = game;
		this.self = self;
		this.side = side;
		this.data = data;
	}

	public Trigger getTrigger() {
		return trigger;
	}

	public EffectParameters getParams() {
		return params;
	}

	public Shoukan getShoukan() {
		return shoukan;
	}

	public Senshi getSelf() {
		return self;
	}

	public Side getSide() {
		return side;
	}

	public Map<String, Object> getData() {
		return data;
	}
}
