package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.user.UserItem;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record Loot(List<Gear> gear, Bag<UserItem> items) {
	public Loot() {
		this(new ArrayList<>(), new TreeBag<>(Comparator.comparing(UserItem::getId)));
	}

	public void add(Loot lt) {
		gear.addAll(lt.gear());
		items.addAll(lt.items());
	}
}
