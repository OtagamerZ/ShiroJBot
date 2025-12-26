package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.dunhun.AreaMap;
import com.kuuhaku.model.persistent.dunhun.Dungeon;
import com.ygimenez.json.JSONArray;

public class DungeonContext extends EffectContext<Dungeon> {
	private final JSONArray monsterPool;
	private final AreaMap map;

	public DungeonContext(Dunhun game, Dungeon dungeon, AreaMap map) {
		super(game, dungeon);
		this.monsterPool = dungeon.getMonsterPool();
		this.map = map;
	}

	public JSONArray getMonsterPool() {
		return monsterPool;
	}

	public AreaMap getMap() {
		return map;
	}
}
