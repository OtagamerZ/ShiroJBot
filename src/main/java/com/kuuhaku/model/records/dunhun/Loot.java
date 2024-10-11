package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.user.UserItem;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public record Loot(Set<Gear> gear, Bag<UserItem> items) {
	public Loot() {
		this(new HashSet<>(), new TreeBag<>(Comparator.comparing(UserItem::getId)));
	}

	public void add(Loot lt) {
		gear.addAll(lt.gear());
		items.addAll(lt.items());
	}
}
