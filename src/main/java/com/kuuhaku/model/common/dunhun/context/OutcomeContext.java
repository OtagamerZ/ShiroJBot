package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.model.common.dunhun.Floor;

public class OutcomeContext extends EffectContext<Floor> {
	public OutcomeContext(Floor floor) {
		super(floor.getMap().getRun().getGame(), floor);
	}
}
