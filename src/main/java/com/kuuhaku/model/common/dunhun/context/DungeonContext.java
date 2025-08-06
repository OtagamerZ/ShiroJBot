package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.persistent.dunhun.Dungeon;
import com.ygimenez.json.JSONArray;

public class DungeonContext extends EffectContext {
	private final JSONArray monsterPool;

	public DungeonContext(Dunhun game, Dungeon dungeon) {
		super(game);
		this.monsterPool = dungeon.getMonsterPool();
	}

	public JSONArray getMonsterPool() {
		return monsterPool;
	}
}
